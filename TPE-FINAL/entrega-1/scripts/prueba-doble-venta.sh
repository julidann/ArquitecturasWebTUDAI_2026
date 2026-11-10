#!/usr/bin/env bash
# Prueba manual de "no doble venta" contra el sistema levantado:
# N clientes distintos intentan reservar EL MISMO asiento al mismo tiempo a traves del gateway.
# Tiene que haber exactamente un 201 y el resto 409.
#
# Uso: ./scripts/prueba-doble-venta.sh <eventoId> <sectorId> <asientoId> [clientes] [url-gateway]
# Ej:  ./scripts/prueba-doble-venta.sh 1 2 15 50
set -euo pipefail

EVENTO=$1; SECTOR=$2; ASIENTO=$3; N=${4:-50}; GW="${5:-http://localhost:8080}/api"
TMP=$(mktemp -d)

for i in $(seq 1 "$N"); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$GW/reservas" \
    -H "Content-Type: application/json" -H "X-Usuario-Id: $((5000 + i))" \
    -d "{\"eventoId\": $EVENTO, \"sectorId\": $SECTOR, \"asientoIds\": [$ASIENTO]}" > "$TMP/$i" &
done
wait

echo "Resultados de $N pedidos simultaneos:"
cat "$TMP"/* | sort | uniq -c
EXITOS=$(cat "$TMP"/* | grep -c '^201$' || true)
rm -rf "$TMP"
if [ "$EXITOS" -eq 1 ]; then echo "OK: el asiento se reservo una sola vez"; else echo "ERROR: hubo $EXITOS reservas exitosas"; exit 1; fi
