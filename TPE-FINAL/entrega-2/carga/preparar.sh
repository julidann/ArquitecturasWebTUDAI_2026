#!/usr/bin/env bash
# Prepara los datos de la prueba de carga: un lugar con un "Campo" de capacidad acotada, un evento
# publicado y N clientes registrados (carga-<prefijo>-1..N@mail.com). Imprime los datos que usa compra.jmx.
#
# Uso: ./carga/preparar.sh [clientes] [capacidad-del-campo] [url-gateway]
set -euo pipefail
N=${1:-200}; CAPACIDAD=${2:-1000}; GW="${3:-http://localhost:8080}"
cd "$(dirname "$0")"
source ../scripts/comun.sh

ADMIN=$(login admin@ticketya.com admin123)
ORG=$(login organizador@ticketya.com organizador123)
SUFIJO=$(date +%s)

LUGAR=$(api POST /lugares "$ADMIN" "{\"nombre\": \"Estadio de la prueba de carga $SUFIJO\", \"direccion\": \"Ruta 226 km 1\",
  \"ciudad\": \"Tandil\", \"latitud\": -37.3, \"longitud\": -59.1,
  \"sectores\": [{\"nombre\": \"Campo\", \"tipo\": \"NO_NUMERADO\", \"capacidad\": $CAPACIDAD}]}")
LUGAR_ID=$(echo "$LUGAR" | jq -r .id); SECTOR=$(echo "$LUGAR" | jq -r '.sectores[0].id')
EVENTO=$(api POST /eventos "$ORG" "{\"nombre\": \"Prueba de carga $SUFIJO\", \"descripcion\": \"Evento para JMeter\",
  \"tipo\": \"RECITAL\", \"participantes\": [\"Banda de la carga\"], \"fechaHora\": \"$(date -d '+40 days' +%Y-%m-%dT21:00:00)\",
  \"lugarId\": $LUGAR_ID, \"limiteEntradasPorCliente\": 20}" | jq -r .id)
api PUT "/eventos/$EVENTO/precios" "$ORG" "{\"precios\": [{\"sectorId\": $SECTOR, \"precio\": 25000}]}" > /dev/null
api POST "/eventos/$EVENTO/publicar" "$ORG" > /dev/null

for i in $(seq 1 "$N"); do
  EMAIL="carga-$SUFIJO-$i@mail.com"
  curl -sf -o /dev/null -X POST "$GW/auth/registro" -H "Content-Type: application/json" \
    -d "{\"email\": \"$EMAIL\", \"nombre\": \"Cliente carga $i\", \"password\": \"cliente123\"}" &
  if (( i % 20 == 0 )); then wait; fi
done
wait
echo "evento=$EVENTO sector=$SECTOR prefijo=$SUFIJO capacidad=$CAPACIDAD clientes=$N" | tee ultima-preparacion.txt
