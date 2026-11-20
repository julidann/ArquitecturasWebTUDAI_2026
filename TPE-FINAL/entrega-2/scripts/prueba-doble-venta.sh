#!/usr/bin/env bash
# Prueba manual de "no doble venta" contra el sistema levantado:
# N clientes distintos (cada uno con su JWT) intentan reservar EL MISMO asiento al mismo tiempo a traves
# del gateway. En docker-compose ms-reservas corre con 2 instancias. Tiene que haber un 201 y el resto 409.
#
# Uso: ./scripts/prueba-doble-venta.sh <eventoId> <sectorId> <asientoId> [clientes] [url-gateway]
# Ej:  ./scripts/prueba-doble-venta.sh 3 4 105 50
set -euo pipefail

EVENTO=$1; SECTOR=$2; ASIENTO=$3; N=${4:-50}; GW="${5:-http://localhost:8080}"
source "$(dirname "$0")/comun.sh"
TMP=$(mktemp -d)

echo "Registrando $N clientes..."
SUFIJO=$(date +%s)
for i in $(seq 1 "$N"); do
  registrar "doble-venta-$SUFIJO-$i@mail.com" "Cliente $i" > "$TMP/token-$i" &
  if (( i % 20 == 0 )); then wait; fi
done
wait

for i in $(seq 1 "$N"); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$GW/reservas" \
    -H "Content-Type: application/json" -H "Authorization: Bearer $(cat "$TMP/token-$i")" \
    -d "{\"eventoId\": $EVENTO, \"sectorId\": $SECTOR, \"asientoIds\": [$ASIENTO]}" > "$TMP/r-$i" &
done
wait

echo "Resultados de $N pedidos simultaneos:"
cat "$TMP"/r-* | sort | uniq -c
EXITOS=$(cat "$TMP"/r-* | grep -c '^201$' || true)
rm -rf "$TMP"
if [ "$EXITOS" -eq 1 ]; then echo "OK: el asiento se reservo una sola vez"; else echo "ERROR: hubo $EXITOS reservas exitosas"; exit 1; fi
