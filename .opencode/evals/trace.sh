#!/usr/bin/env bash
# trace.sh — Append a validated JSON trace line to a dated JSONL file.
#
# Usage:
#   ./trace.sh <agent_type> '<json_body>'
#   echo '<json_body>' | ./trace.sh <agent_type> --stdin
#   ./trace.sh <agent_type> '<json_body>' --session <session_id>
#
# The json_body is merged into the trace envelope under top-level keys.
#
# Output: .opencode/evals/traces/<session_id>-<yyyy-mm-dd>.jsonl

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TRACES_DIR="$SCRIPT_DIR/traces"
SESSION_ID="${SESSION_ID:-default}"
DATE=$(date +%Y-%m-%d)
mkdir -p "$TRACES_DIR"

AGENT_TYPE="${1:?Usage: trace.sh <agent_type> <json>}"
shift

if [ "$1" = "--stdin" ]; then
    TRACE_JSON=$(cat)
    shift
else
    TRACE_JSON="$1"
    shift
fi

while [ $# -gt 0 ]; do
    case "$1" in
        --session) SESSION_ID="$2"; shift 2;;
        *) echo "Unknown: $1"; exit 1;;
    esac
done

TRACE_FILE="$TRACES_DIR/$SESSION_ID-$DATE.jsonl"

python3 -c "
import json, sys
from datetime import datetime, timezone

agent_type = '$AGENT_TYPE'
session_id = '$SESSION_ID'
trace_id = 'trace-$(date +%s)-$$'

body = json.loads('''$TRACE_JSON''')

envelope = {
    'schema_version': '1.0',
    'trace_id': trace_id,
    'session_id': session_id,
    'timestamp': datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ'),
    'agent': {'type': agent_type}
}

for k, v in body.items():
    envelope[k] = v

line = json.dumps(envelope, ensure_ascii=False)

with open('$TRACE_FILE', 'a') as f:
    f.write(line + '\n')

print(f'TRACE: {agent_type} -> {session_id}-$DATE.jsonl', file=sys.stderr)
" || exit 1
