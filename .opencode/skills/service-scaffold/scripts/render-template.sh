#!/usr/bin/env bash
# Renders a template file by replacing __PLACEHOLDERS__ with values.
# Handles conditional blocks: __IF_*__ ... __END_IF__ (or # __IF_*__ ... # __END_IF__ for YAML).
#
# Usage: ./render-template.sh <template-file> <output-file> [key=value ...]
#
# Conditional markers can be standalone (XML/Java) or comment-prefixed (YAML):
#   __IF_POSTGRESQL__
#   content...
#   __END_IF__
# or:
#   # __IF_POSTGRESQL__
#   content...
#   # __END_IF__

set -euo pipefail

TEMPLATE="${1:?Usage: $0 <template> <output> [key=value ...]}"
OUTPUT="${2:?Usage: $0 <template> <output> [key=value ...]}"
shift 2

ARGS=("$@")

# Build sed expression from key=value pairs
SED_EXPR=""
for arg in "${ARGS[@]}"; do
    key="${arg%%=*}"
    value="${arg#*=}"
    value="${value//\//\\/}"
    SED_EXPR="${SED_EXPR} -e 's/__${key}__/${value}/g'"
done

# Extract feature flags from args
DB_TYPE=""
HAS_KAFKA=true
HAS_EUREKA=true
for arg in "${ARGS[@]}"; do
    case "${arg%%=*}" in
        DB_TYPE)    DB_TYPE="${arg#*=}" ;;
        HAS_KAFKA)  [[ "${arg#*=}" == "false" ]] && HAS_KAFKA=false ;;
        HAS_EUREKA) [[ "${arg#*=}" == "false" ]] && HAS_EUREKA=false ;;
    esac
done

process_conditionals() {
    local db="$1"
    local file="$2"

    for cond in POSTGRESQL ELASTICSEARCH KAFKA EUREKA; do
        local active=false
        case "${cond}" in
            POSTGRESQL)    [[ "${db}" == "postgresql" ]] && active=true ;;
            ELASTICSEARCH) [[ "${db}" == "elasticsearch" ]] && active=true ;;
            KAFKA)         $HAS_KAFKA && active=true ;;
            EUREKA)        $HAS_EUREKA && active=true ;;
        esac

        if $active; then
            # Remove marker lines, keep content
            sed -i \
                -e "/^__IF_${cond}__$/d" \
                -e "/^__END_IF__$/d" \
                -e "/^# __IF_${cond}__$/d" \
                -e "/^# __END_IF__$/d" \
                "${file}"
        else
            # Remove from opening marker to closing marker (inclusive)
            sed -i \
                -e "/^__IF_${cond}__$/,/^__END_IF__$/d" \
                -e "/^# __IF_${cond}__$/,/^# __END_IF__$/d" \
                "${file}"
        fi
    done
}

cp "${TEMPLATE}" "${OUTPUT}"
eval "sed -i ${SED_EXPR} \"${OUTPUT}\""
process_conditionals "${DB_TYPE}" "${OUTPUT}"
sed -i 's/__[A-Z_]*__//g' "${OUTPUT}"
echo "Rendered ${TEMPLATE} → ${OUTPUT}"
