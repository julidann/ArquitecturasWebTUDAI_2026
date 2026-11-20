#!/usr/bin/env bash
# Corre la prueba de carga del flujo de compra con JMeter (modo consola) y genera el reporte HTML.
# Usa el JMeter instalado (variable JMETER o "jmeter" en el PATH); si no hay, usa la imagen de Docker.
#
# Uso: ./carga/ejecutar.sh [usuarios] [iteraciones] [rampa-seg] [url-gateway]
#      (toma evento, sector y prefijo de carga/ultima-preparacion.txt, que deja preparar.sh)
set -euo pipefail
cd "$(dirname "$0")"
USUARIOS=${1:-200}; ITER=${2:-5}; RAMPA=${3:-30}; GW="${4:-http://localhost:8080}"
eval "$(grep -o '[a-z]*=[0-9]*' ultima-preparacion.txt | sed 's/^/PREP_/')"
EVENTO=$PREP_evento; SECTOR=$PREP_sector; PREFIJO=$PREP_prefijo
HOST=$(echo "$GW" | sed -E 's#https?://([^:/]+).*#\1#'); PUERTO=$(echo "$GW" | sed -E 's#.*:([0-9]+).*#\1#')
SALIDA="resultados/$(date +%Y%m%d-%H%M%S)"; mkdir -p "$SALIDA"
ARGS=(-n -t compra.jmx -l "$SALIDA/resultados.jtl" -e -o "$SALIDA/reporte"
      -Jhost="$HOST" -Jpuerto="$PUERTO" -Jevento="$EVENTO" -Jsector="$SECTOR" -Jprefijo="$PREFIJO"
      -Jusuarios="$USUARIOS" -Jiteraciones="$ITER" -Jrampa="$RAMPA")
JM=${JMETER:-$(command -v jmeter || true)}
if [ -n "$JM" ]; then
  "$JM" "${ARGS[@]}"
else
  docker run --rm --network host -v "$PWD:/carga" -w /carga justb4/jmeter:5.5 "${ARGS[@]}"
fi
echo "Resultados en carga/$SALIDA (abrir reporte/index.html)"
