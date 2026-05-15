#!/usr/bin/env bash
# Creates the standard Eventixx microservice directory structure.
# Usage: ./create-directories.sh <service-name> <database-type>
#   database-type: postgresql | elasticsearch | none

set -euo pipefail

SERVICE_NAME="${1:?Usage: $0 <service-name> <database-type (postgresql|elasticsearch|none)>}"
DB_TYPE="${2:?Usage: $0 <service-name> <database-type (postgresql|elasticsearch|none)>}"

FLAT_NAME="${SERVICE_NAME//-/}"
FLAT_NAME="${FLAT_NAME//./}"

BASE="services/${SERVICE_NAME}"
PACKAGE_DIR="${BASE}/src/main/java/com/eventixx/${FLAT_NAME}"
TEST_DIR="${BASE}/src/test/java/com/eventixx/${FLAT_NAME}"

# Main source directories
mkdir -p "${PACKAGE_DIR}/config"
mkdir -p "${PACKAGE_DIR}/controllers"
mkdir -p "${PACKAGE_DIR}/dto"
mkdir -p "${PACKAGE_DIR}/entities"
mkdir -p "${PACKAGE_DIR}/exceptions"
mkdir -p "${PACKAGE_DIR}/mappers"
mkdir -p "${PACKAGE_DIR}/repositories"
mkdir -p "${PACKAGE_DIR}/services/messaging"
mkdir -p "${BASE}/src/main/resources"

# Conditional directories
if [[ "${DB_TYPE}" == "postgresql" ]]; then
    mkdir -p "${BASE}/src/main/resources/db/migration"
fi

# Test directories
mkdir -p "${TEST_DIR}/arch"
mkdir -p "${TEST_DIR}/integration"
mkdir -p "${TEST_DIR}/unit"
mkdir -p "${TEST_DIR}/web"

if [[ "${DB_TYPE}" == "postgresql" ]]; then
    mkdir -p "${TEST_DIR}/repository"
fi

echo "Created directory structure for ${SERVICE_NAME} (flat: ${FLAT_NAME})"
