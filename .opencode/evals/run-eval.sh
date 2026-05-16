#!/usr/bin/env bash
# run-eval.sh — Execute a single eval scenario and capture trace data.
#
# Usage:
#   ./run-eval.sh <eval-id>           # Run one scenario
#   ./run-eval.sh --list              # List all scenarios
#   ./run-eval.sh --all               # Run all auto scenarios
#   ./run-eval.sh QR-01               # Run quality-runner scenario
#
# Auto scenarios (can run without agent invocation)
# Manual scenarios (require orchestrator/subagent delegation)
#   ORC-*  → orchestrator scenarios
#   CW-*   → code-writer scenarios
#   LIB-*  → librarian scenarios
#   DU-*   → docs-updater scenarios

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TRACES_DIR="$SCRIPT_DIR/traces"

if [ $# -eq 0 ]; then
    echo "Usage: $0 <eval-id> | --list | --all"
    exit 1
fi

EVAL_ID="${1:-}"

list_scenarios() {
    echo "=== Available Eval Scenarios ==="
    echo ""
    echo "AUTO (can run without agents):"
    echo "  QR-01  Clean build (no violations)"
    echo "  QR-02  Checkstyle violation detection"
    echo "  QR-03  PMD complexity violation"
    echo "  QR-04  Test failure detection"
    echo "  QR-05  Mixed violations (fast-fail check)"
    echo ""
    echo "MANUAL (requires agent execution):"
    echo "  ORC-01  Add GET endpoint to event-catalog"
    echo "  ORC-02  Create a new service"
    echo "  ORC-03  Fix soft delete bug in reservation"
    echo "  ORC-04  Update MapStruct version"
    echo "  ORC-05  Research Kafka consumer pattern"
    echo "  ORC-06  Add POST endpoint for ticket-types"
    echo "  ORC-07  Review event-catalog PR"
    echo "  ORC-08  Implement search by category"
    echo "  ORC-09  Fix Checkstyle violation"
    echo "  ORC-10  Sync docs after entity migration"
    echo "  CW-01   New controller + DTO + service"
    echo "  CW-02   New entity + repository"
    echo "  CW-03   Bug fix (complexity violation)"
    echo "  CW-04   Service with Kafka producer"
    echo "  CW-05   Test class for existing endpoint"
    echo "  LIB-01  @MockitoBean signature"
    echo "  LIB-02  Kafka transactional producer"
    echo "  LIB-03  JPA Specification pagination"
    echo "  LIB-04  SecurityFilterChain migration"
    echo "  LIB-05  MapStruct version diff"
    echo "  DU-01   New entity added"
    echo "  DU-02   New endpoint added"
    echo "  DU-03   Dependency version bump"
    echo "  DU-04   Private method change (no doc)"
    echo "  DU-05   Security config change"
    echo ""
    echo "Usage: $0 <eval-id>"
    echo "Example: $0 QR-01"
}

if [ "$EVAL_ID" = "--list" ]; then
    list_scenarios
    exit 0
fi

if [ "$EVAL_ID" = "--all" ]; then
    echo "Running all AUTO scenarios..."
    for SCENARIO in QR-01 QR-02 QR-03 QR-04 QR-05; do
        echo ""
        echo "================================================"
        echo "Running: $SCENARIO"
        echo "================================================"
        "$0" "$SCENARIO" || echo "WARNING: $SCENARIO failed"
    done
    echo ""
    echo "All AUTO scenarios complete."
    echo ""
    echo "MANUAL scenarios remain. Execute each individually:"
    echo "  ORC-01 through ORC-10, CW-01 through CW-05,"
    echo "  LIB-01 through LIB-05, DU-01 through DU-05"
    echo ""
    "$SCRIPT_DIR/collect-traces.sh" --session "eval-suite-$(date +%Y-%m-%d)" --report
    exit 0
fi

# ============================================================
# Scenario definitions
# ============================================================

run_qr_01() {
    echo "[QR-01] Clean build (no violations)"
    local SERVICE="${1:-event-catalog-service}"
    local START_TS
    START_TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    local START_MS
    START_MS=$(date +%s%3N)

    if [ -f "./mvnw" ]; then
        MVN="./mvnw"
    else
        MVN="mvn"
    fi

    set +e
    OUTPUT=$($MVN verify -pl "services/$SERVICE" -q 2>&1)
    EXIT_CODE=$?
    set -e

    local END_MS
    END_MS=$(date +%s%3N)
    local DURATION_MS=$((END_MS - START_MS))
    local STATUS="success"
    [ $EXIT_CODE -ne 0 ] && STATUS="fail"

    TRACE_JSON=$(cat <<EOF
{
  "task": {
    "input": "Run mvn verify on $SERVICE expecting clean build",
    "intent": "test",
    "complexity": "low",
    "eval_id": "QR-01"
  },
  "execution": {
    "status": "$STATUS",
    "duration_ms": $DURATION_MS,
    "tokens_in": 0,
    "tokens_out": 0,
    "quality_passed": $( [ $EXIT_CODE -eq 0 ] && echo "true" || echo "false"),
    "tool_results": {
      "verify": "$([ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL")"
    }
  },
  "result": {
    "files_created": [],
    "files_modified": [],
    "error": $( [ $EXIT_CODE -eq 0 ] && echo "null" || echo "\"Maven verify failed (exit $EXIT_CODE)\"")
  }
}
EOF
)
    echo "$TRACE_JSON" | "$SCRIPT_DIR/trace.sh" quality-runner --stdin --session "eval-qr01"
    echo "[QR-01] $STATUS (${DURATION_MS}ms)"
}

run_qr_02() {
    echo "[QR-02] Checkstyle violation detection"
    local SERVICE="${1:-event-catalog-service}"
    local START_MS
    START_MS=$(date +%s%3N)

    if [ -f "./mvnw" ]; then
        MVN="./mvnw"
    else
        MVN="mvn"
    fi

    set +e
    OUTPUT=$($MVN checkstyle:check -pl "services/$SERVICE" -DskipTests 2>&1)
    EXIT_CODE=$?
    set -e

    local END_MS
    END_MS=$(date +%s%3N)
    local DURATION_MS=$((END_MS - START_MS))
    local VIOLATIONS
    VIOLATIONS=$(echo "$OUTPUT" | grep -c "Checkstyle" 2>/dev/null || echo "0")
    local STATUS="success"
    [ $EXIT_CODE -ne 0 ] && STATUS="fail"

    TRACE_JSON=$(cat <<EOF
{
  "task": {
    "input": "Run checkstyle:check on $SERVICE to detect violations",
    "intent": "test",
    "complexity": "low",
    "eval_id": "QR-02"
  },
  "execution": {
    "status": "$STATUS",
    "duration_ms": $DURATION_MS,
    "tokens_in": 0,
    "tokens_out": 0,
    "quality_passed": $( [ $EXIT_CODE -eq 0 ] && echo "true" || echo "false"),
    "tool_results": {
      "checkstyle": "$([ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL")"
    }
  },
  "result": {
    "violations_found": $VIOLATIONS,
    "error": $( [ $EXIT_CODE -eq 0 ] && echo "null" || echo "\"Checkstyle found $VIOLATIONS violations (exit $EXIT_CODE)\"")
  }
}
EOF
)
    echo "$TRACE_JSON" | "$SCRIPT_DIR/trace.sh" quality-runner --stdin --session "eval-qr02"
    echo "[QR-02] $STATUS (${DURATION_MS}ms, violations: $VIOLATIONS)"
}

run_qr_03() {
    echo "[QR-03] PMD complexity violation (PMD check, not CPD)"
    local SERVICE="${1:-event-catalog-service}"
    local START_MS
    START_MS=$(date +%s%3N)

    if [ -f "./mvnw" ]; then
        MVN="./mvnw"
    else
        MVN="mvn"
    fi

    set +e
    OUTPUT=$($MVN pmd:check -pl "services/$SERVICE" -DskipTests 2>&1)
    EXIT_CODE=$?
    set -e

    local END_MS
    END_MS=$(date +%s%3N)
    local DURATION_MS=$((END_MS - START_MS))
    local STATUS="success"
    [ $EXIT_CODE -ne 0 ] && STATUS="fail"

    TRACE_JSON=$(cat <<EOF
{
  "task": {
    "input": "Run pmd:check on $SERVICE to detect complexity violations",
    "intent": "test",
    "complexity": "low",
    "eval_id": "QR-03"
  },
  "execution": {
    "status": "$STATUS",
    "duration_ms": $DURATION_MS,
    "tokens_in": 0,
    "tokens_out": 0,
    "quality_passed": $( [ $EXIT_CODE -eq 0 ] && echo "true" || echo "false"),
    "tool_results": {
      "pmd": "$([ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL")"
    }
  },
  "result": {
    "error": $( [ $EXIT_CODE -eq 0 ] && echo "null" || echo "\"PMD violations detected (exit $EXIT_CODE)\"")
  }
}
EOF
)
    echo "$TRACE_JSON" | "$SCRIPT_DIR/trace.sh" quality-runner --stdin --session "eval-qr03"
    echo "[QR-03] $STATUS (${DURATION_MS}ms)"
}

run_qr_04() {
    echo "[QR-04] Test failure detection"
    local SERVICE="${1:-event-catalog-service}"
    local START_MS
    START_MS=$(date +%s%3N)

    if [ -f "./mvnw" ]; then
        MVN="./mvnw"
    else
        MVN="mvn"
    fi

    set +e
    OUTPUT=$($MVN test -pl "services/$SERVICE" 2>&1)
    EXIT_CODE=$?
    set -e

    local END_MS
    END_MS=$(date +%s%3N)
    local DURATION_MS=$((END_MS - START_MS))

    local TESTS_PASSED
    TESTS_PASSED=$(echo "$OUTPUT" | grep -oP 'Tests run: \K\d+' 2>/dev/null | tail -1 || echo "0")
    local TESTS_FAILED
    TESTS_FAILED=$(echo "$OUTPUT" | grep -oP 'Failures: \K\d+' 2>/dev/null | tail -1 || echo "0")
    local TESTS_ERRORS
    TESTS_ERRORS=$(echo "$OUTPUT" | grep -oP 'Errors: \K\d+' 2>/dev/null | tail -1 || echo "0")
    local STATUS="success"
    [ $EXIT_CODE -ne 0 ] && STATUS="fail"

    TRACE_JSON=$(cat <<EOF
{
  "task": {
    "input": "Run tests on $SERVICE to verify test pass/fail detection",
    "intent": "test",
    "complexity": "low",
    "eval_id": "QR-04"
  },
  "execution": {
    "status": "$STATUS",
    "duration_ms": $DURATION_MS,
    "tokens_in": 0,
    "tokens_out": 0,
    "quality_passed": $( [ $EXIT_CODE -eq 0 ] && echo "true" || echo "false"),
    "tool_results": {
      "tests": "$([ $EXIT_CODE -eq 0 ] && echo "PASS" || echo "FAIL")"
    }
  },
  "result": {
    "tests_run": $TESTS_PASSED,
    "tests_failed": $TESTS_FAILED,
    "tests_errors": $TESTS_ERRORS,
    "error": $( [ $EXIT_CODE -eq 0 ] && echo "null" || echo "\"Test failures detected (exit $EXIT_CODE)\"")
  }
}
EOF
)
    echo "$TRACE_JSON" | "$SCRIPT_DIR/trace.sh" quality-runner --stdin --session "eval-qr04"
    echo "[QR-04] $STATUS (${DURATION_MS}ms, run: $TESTS_PASSED, fail: $TESTS_FAILED, err: $TESTS_ERRORS)"
}

run_qr_05() {
    echo "[QR-05] Mixed violations (fast-fail: spotless + checkstyle + pmd + spotbugs)"
    local SERVICE="${1:-event-catalog-service}"
    local START_MS
    START_MS=$(date +%s%3N)

    if [ -f "./mvnw" ]; then
        MVN="./mvnw"
    else
        MVN="mvn"
    fi

    set +e
    STEP0_OUTPUT=$($MVN spotless:apply -pl "services/$SERVICE" -q 2>&1 && $MVN checkstyle:check pmd:check pmd:cpd-check spotbugs:check -pl "services/$SERVICE" -DskipTests -q 2>&1)
    STEP0_EXIT=$?

    if [ $STEP0_EXIT -eq 0 ]; then
        FULL_OUTPUT=$($MVN verify -pl "services/$SERVICE" 2>&1)
        FULL_EXIT=$?
    else
        FULL_EXIT=$STEP0_EXIT
    fi
    set -e

    local END_MS
    END_MS=$(date +%s%3N)
    local DURATION_MS=$((END_MS - START_MS))
    local STATUS="success"
    [ $FULL_EXIT -ne 0 ] && STATUS="fail"

    TRACE_JSON=$(cat <<EOF
{
  "task": {
    "input": "Run full fast-fail pipeline on $SERVICE (spotless → checkstyle → pmd → spotbugs → tests)",
    "intent": "test",
    "complexity": "low",
    "eval_id": "QR-05"
  },
  "execution": {
    "status": "$STATUS",
    "duration_ms": $DURATION_MS,
    "tokens_in": 0,
    "tokens_out": 0,
    "quality_passed": $( [ $FULL_EXIT -eq 0 ] && echo "true" || echo "false"),
    "tool_results": {
      "fast_lint": "$([ $STEP0_EXIT -eq 0 ] && echo "PASS" || echo "FAIL")",
      "full_verify": "$([ $FULL_EXIT -eq 0 ] && echo "PASS" || echo "FAIL")"
    }
  },
  "result": {
    "error": $( [ $FULL_EXIT -eq 0 ] && echo "null" || echo "\"Pipeline failed at step \$([ \$STEP0_EXIT -ne 0 ] && echo 'fast-lint' || echo 'verify')\"")
  }
}
EOF
)
    echo "$TRACE_JSON" | "$SCRIPT_DIR/trace.sh" quality-runner --stdin --session "eval-qr05"
    echo "[QR-05] $STATUS (${DURATION_MS}ms)"
}

# ============================================================
# Manual scenario placeholders (print instructions)
# ============================================================

run_orc() {
    local ID="$1"
    echo "[$ID] MANUAL — Requires orchestrator + subagent delegation."
    echo ""
    echo "How to run:"
    echo "  1. Invoke the orchestrator with the scenario request"
    echo "  2. Orchestrator decomposes, delegates, and closes"
    echo "  3. Trace data is automatically collected by trace-collector"
    echo ""
    echo "Scenario descriptions:"
    case "$ID" in
        ORC-01) echo "  Request: 'Add GET /api/v1/events/{id} endpoint to event-catalog-service'";;
        ORC-02) echo "  Request: 'Create a new reservation-service from scratch'";;
        ORC-03) echo "  Request: 'Fix soft delete bug in reservation-service — deleted records still appear in queries'";;
        ORC-04) echo "  Request: 'Update MapStruct from 1.6.0 to 1.7.0'";;
        ORC-05) echo "  Request: 'Research best Kafka consumer pattern for Spring Boot 4.0'";;
        ORC-06) echo "  Request: 'Adicionar endpoint POST /api/v1/ticket-types no event-catalog'";;
        ORC-07) echo "  Request: 'Review the event-catalog PR for convention violations'";;
        ORC-08) echo "  Request: 'Implement search by category with pagination in event-catalog'";;
        ORC-09) echo "  Request: 'Fix Checkstyle violation in PaymentService — line too long'";;
        ORC-10) echo "  Request: 'Sincronizar docs apos migracao de entidade TicketType'";;
    esac
}

run_cw() {
    local ID="$1"
    echo "[$ID] MANUAL — Requires code-writer delegation from orchestrator."
    echo "Invoke orchestrator with the corresponding request."
    case "$ID" in
        CW-01) echo "  Request: 'Create EventController + EventResponse + EventService for GET /api/v1/events'";;
        CW-02) echo "  Request: 'Create TicketType entity + TicketTypeRepository with soft delete'";;
        CW-03) echo "  Request: 'Fix PMD CyclomaticComplexity > 10 in SearchService.search()'";;
        CW-04) echo "  Request: 'Add KafkaTemplate producer to ReservationService for reservation.created event'";;
        CW-05) echo "  Request: 'Write tests for GET /api/v1/events/{id} using @MockitoBean and RestTestClient'";;
    esac
}

run_lib() {
    local ID="$1"
    echo "[$ID] MANUAL — Requires librarian delegation from orchestrator."
    echo "Invoke orchestrator with the corresponding research request."
    case "$ID" in
        LIB-01) echo "  Request: 'Research @MockitoBean in Spring Boot 4 — correct import and usage'";;
        LIB-02) echo "  Request: 'Research Kafka transactional producer in Spring Boot 4.0'";;
        LIB-03) echo "  Request: 'Research JPA Specification with pagination — best practices 2026'";;
        LIB-04) echo "  Request: 'Research SecurityFilterChain migration from WebSecurityConfigurerAdapter'";;
        LIB-05) echo "  Request: 'Research MapStruct 1.6.0 vs 1.7.0 breaking changes'";;
    esac
}

run_du() {
    local ID="$1"
    echo "[$ID] MANUAL — Requires docs-updater after code changes."
    case "$ID" in
        DU-01) echo "  Trigger: 'Add TicketType entity with id UUID, name VARCHAR(100), price DECIMAL(10,2), event_id FK'";;
        DU-02) echo "  Trigger: 'Add GET /api/v1/ticket-types/{id} and POST /api/v1/ticket-types to EventController'";;
        DU-03) echo "  Trigger: 'Bump MapStruct from 1.6.0 to 1.7.0 in parent pom.xml'";;
        DU-04) echo "  Trigger: 'Rename private method in EventService (no doc impact expected)'";;
        DU-05) echo "  Trigger: 'Add 2FA SecurityFilterChain for /api/v1/admin/ endpoints in SecurityConfig'";;
    esac
}

# ============================================================
# Dispatch
# ============================================================

case "$EVAL_ID" in
    QR-01) run_qr_01 "${2:-event-catalog-service}" ;;
    QR-02) run_qr_02 "${2:-event-catalog-service}" ;;
    QR-03) run_qr_03 "${2:-event-catalog-service}" ;;
    QR-04) run_qr_04 "${2:-event-catalog-service}" ;;
    QR-05) run_qr_05 "${2:-event-catalog-service}" ;;
    ORC-*) run_orc "$EVAL_ID" ;;
    CW-*)  run_cw "$EVAL_ID" ;;
    LIB-*) run_lib "$EVAL_ID" ;;
    DU-*)  run_du "$EVAL_ID" ;;
    --all) ;;
    *) echo "Unknown eval: $EVAL_ID"; list_scenarios; exit 1;;
esac
