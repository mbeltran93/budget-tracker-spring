#!/usr/bin/env bash
# Unico comando para correr la suite de Playwright de punta a punta:
# levanta app + Postgres con docker compose (build incluido), espera a que el
# login responda, instala los browsers de Playwright si hace falta, corre los
# tests, y al final apaga y borra los contenedores + el volumen, pase lo que pase.
set -euo pipefail

cd "$(dirname "$0")/.."
COMPOSE="docker compose"

cleanup() {
    echo "Apagando app + base de datos..."
    $COMPOSE down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "Levantando app + Postgres (docker compose up --build)..."
$COMPOSE up -d --build

BASE_URL="${BASE_URL:-http://localhost:8080}"
echo "Esperando a que la app responda en ${BASE_URL}/login..."
for _ in $(seq 1 60); do
    if curl -s -o /dev/null "${BASE_URL}/login"; then
        echo "App lista."
        break
    fi
    sleep 1
done

cd e2e
if [ ! -d node_modules ]; then
    echo "Instalando dependencias de Playwright..."
    npm install
fi
npx playwright install --with-deps chromium

BASE_URL="${BASE_URL}" npx playwright test
