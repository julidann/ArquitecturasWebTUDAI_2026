#!/usr/bin/env bash
# Corre los tests de integracion entre microservicios contra el sistema levantado con docker compose.
# Uso: ./scripts/tests-integracion.sh [url-del-gateway]
set -euo pipefail
cd "$(dirname "$0")/.."
GW="${1:-http://localhost:8080}"
echo "Esperando a que el gateway responda en $GW ..."
for _ in $(seq 1 60); do curl -sf "$GW/actuator/health" > /dev/null && break; sleep 5; done
cd tests-integracion && mvn -B verify -Dgateway.url="$GW"
