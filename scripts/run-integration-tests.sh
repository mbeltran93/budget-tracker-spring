#!/usr/bin/env bash
# Levanta un Postgres descartable con docker, corre los tests de Maven (unitarios +
# integracion con @SpringBootTest/MockMvc) y lo apaga al terminar, pase lo que pase.
set -euo pipefail

CONTAINER_NAME="budget-tracker-test-db"
DB_PORT="${TEST_DB_PORT:-55432}"
DB_NAME="${TEST_DB_NAME:-budget_tracker_test}"
DB_USER="${TEST_DB_USER:-test_user}"
DB_PASSWORD="${TEST_DB_PASSWORD:-test_pass}"

cleanup() {
    docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

cleanup

echo "Levantando Postgres de test en el puerto ${DB_PORT}..."
docker run -d --rm \
    --name "${CONTAINER_NAME}" \
    -e POSTGRES_DB="${DB_NAME}" \
    -e POSTGRES_USER="${DB_USER}" \
    -e POSTGRES_PASSWORD="${DB_PASSWORD}" \
    -p "${DB_PORT}:5432" \
    postgres:16-alpine >/dev/null

echo "Esperando a que Postgres este listo..."
for _ in $(seq 1 30); do
    if docker exec "${CONTAINER_NAME}" pg_isready -U "${DB_USER}" -d "${DB_NAME}" >/dev/null 2>&1; then
        echo "Postgres listo."
        break
    fi
    sleep 1
done

export TEST_DB_PORT="${DB_PORT}"
export TEST_DB_NAME="${DB_NAME}"
export TEST_DB_USER="${DB_USER}"
export TEST_DB_PASSWORD="${DB_PASSWORD}"

cd "$(dirname "$0")/.."
mvn test
