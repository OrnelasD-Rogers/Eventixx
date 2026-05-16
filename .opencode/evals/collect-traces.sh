#!/usr/bin/env bash
# collect-traces.sh — Post-session: aggregate, validate, and report trace files.
#
# Usage:
#   ./collect-traces.sh                          # Summarize latest traces
#   ./collect-traces.sh --session <session>      # Filter by session
#   ./collect-traces.sh --date 2026-05-16        # Filter by date
#   ./collect-traces.sh --report                 # Generate R_geral report

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TRACES_DIR="$SCRIPT_DIR/traces"
REPORTS_DIR="$SCRIPT_DIR/reports"
SESSION_ID="${SESSION_ID:-default}"
DATE=$(date +%Y-%m-%d)
mkdir -p "$REPORTS_DIR"

FILTER_SESSION=""
FILTER_DATE=""
GENERATE_REPORT=false

while [ $# -gt 0 ]; do
    case "$1" in
        --session) FILTER_SESSION="$2"; shift 2;;
        --date) FILTER_DATE="$2"; shift 2;;
        --report) GENERATE_REPORT=true; shift;;
        *) echo "Usage: $0 [--session <id>] [--date <date>] [--report]"; exit 1;;
    esac
done

REPORT_SESSION="${FILTER_SESSION:-$SESSION_ID}"
REPORT_DATE="${FILTER_DATE:-$DATE}"

if [ -n "$FILTER_SESSION" ] && [ -n "$FILTER_DATE" ]; then
    PATTERN="$FILTER_SESSION*$FILTER_DATE.jsonl"
elif [ -n "$FILTER_SESSION" ]; then
    PATTERN="$FILTER_SESSION*.jsonl"
elif [ -n "$FILTER_DATE" ]; then
    PATTERN="*$FILTER_DATE.jsonl"
else
    PATTERN="$SESSION_ID*.jsonl"
fi

echo "=== Trace Collection Report ==="
echo "Session: ${FILTER_SESSION:-$SESSION_ID}"
echo "Date:    ${FILTER_DATE:-$DATE}"
echo "Pattern: $PATTERN"
echo ""

MATCHING_FILES=$(find "$TRACES_DIR" -name "$PATTERN" -type f 2>/dev/null | sort)
if [ -z "$MATCHING_FILES" ]; then
    echo "No trace files found matching pattern."
    echo ""
    echo "Available trace files:"
    find "$TRACES_DIR" -name "*.jsonl" -type f 2>/dev/null | sort || echo "  (none)"
    exit 0
fi

TOTAL_TRACES=0
declare -A COUNTS_A
declare -A COUNTS_S

for TRACE_FILE in $MATCHING_FILES; do
    echo "File: $(basename "$TRACE_FILE")"
    echo "Size: $(wc -c < "$TRACE_FILE" | tr -d ' ') bytes"

    LINE_COUNT=$(wc -l < "$TRACE_FILE" | tr -d ' ')
    echo "Traces: $LINE_COUNT"
    TOTAL_TRACES=$((TOTAL_TRACES + LINE_COUNT))

    while IFS= read -r LINE; do
        if [ -n "$LINE" ]; then
            AGENT_TYPE=$(echo "$LINE" | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('agent',{}).get('type','unknown'))" 2>/dev/null || true)
            COUNTS_A["$AGENT_TYPE"]=$((${COUNTS_A["$AGENT_TYPE"]:-0} + 1))

            STATUS=$(echo "$LINE" | python3 -c "import sys,json; d=json.loads(sys.stdin.read()); print(d.get('execution',{}).get('status','unknown'))" 2>/dev/null || true)
            COUNTS_S["$STATUS"]=$((${COUNTS_S["$STATUS"]:-0} + 1))
        fi
    done < "$TRACE_FILE"

    echo ""
done

echo "=== Summary ==="
echo "Total trace files: $(echo "$MATCHING_FILES" | wc -l)"
echo "Total traces: $TOTAL_TRACES"
echo ""

echo "By Agent Type:"
for AGENT in "${!COUNTS_A[@]}"; do
    echo "  $AGENT: ${COUNTS_A[$AGENT]}"
done | sort

echo ""
echo "By Status:"
for STATUS in success fail error timeout; do
    COUNT=${COUNTS_S[$STATUS]:-0}
    echo "  $STATUS: $COUNT"
done

if [ "$GENERATE_REPORT" = true ] && [ "$TOTAL_TRACES" -gt 0 ]; then
    echo ""
    echo "=== R_geral Report ==="
    echo "Generating reliability report..."

    TRACES_DIR_EXPORT="$TRACES_DIR"
    PATTERN_EXPORT="$PATTERN"
    SESSION_EXPORT="$REPORT_SESSION"
    DATE_EXPORT="$REPORT_DATE"
    REPORT_FILE="$REPORTS_DIR/report-$REPORT_SESSION-$REPORT_DATE.json"

    python3 -c "
import json, os, glob, sys
from collections import defaultdict

traces_dir = '$TRACES_DIR_EXPORT'
pattern = '$PATTERN_EXPORT'
session_id = '$SESSION_EXPORT'
date_str = '$DATE_EXPORT'
report_file = '$REPORT_FILE'

traces = []
for fpath in glob.glob(os.path.join(traces_dir, pattern)):
    with open(fpath) as f:
        for line in f:
            line = line.strip()
            if line:
                traces.append(json.loads(line))

if not traces:
    print('No traces to analyze.')
    sys.exit(0)

by_agent = defaultdict(list)
for t in traces:
    agent_type = t.get('agent', {}).get('type', 'unknown')
    by_agent[agent_type].append(t)

metrics = {}
for agent_type, agent_traces in by_agent.items():
    n = len(agent_traces)
    successes = sum(1 for t in agent_traces if t.get('execution', {}).get('status') == 'success')
    total_dur = sum(t.get('execution', {}).get('duration_ms', 0) for t in agent_traces)
    avg_dur = total_dur / n if n > 0 else 0
    tot_in = sum(t.get('execution', {}).get('tokens_in', 0) for t in agent_traces)
    tot_out = sum(t.get('execution', {}).get('tokens_out', 0) for t in agent_traces)

    metrics[agent_type] = {
        'traces': n,
        'successes': successes,
        'success_rate': round(successes / n * 100, 1) if n > 0 else 0,
        'avg_duration_ms': round(avg_dur, 0),
        'total_tokens_in': tot_in,
        'total_tokens_out': tot_out,
        'total_tokens': tot_in + tot_out
    }

weights = {
    'orchestrator': 0.30,
    'code-writer': 0.25,
    'librarian': 0.15,
    'quality-runner': 0.10,
    'docs-updater': 0.10,
}
r_scores = {}
for agent_type, m in metrics.items():
    r_scores[agent_type] = m['success_rate']

r_geral = round(sum(r_scores.get(a, 0) * weights.get(a, 0) for a in weights), 1)

report = {
    'session_id': session_id,
    'date': date_str,
    'total_traces': len(traces),
    'r_geral': r_geral,
    'per_agent': metrics,
    'r_scores': r_scores,
    'weights_used': weights
}

with open(report_file, 'w') as f:
    json.dump(report, f, indent=2)

print(f'R_geral: {r_geral}%')
for a, s in sorted(r_scores.items()):
    w = weights.get(a, 0)
    print(f'  {a}: {s}% (weight: {w}, contrib: {round(s * w, 1)} pp)')
print(f'Report saved: {report_file}')
" || echo "ERROR: Python report generation failed"
fi

echo ""
echo "=== Done ==="
