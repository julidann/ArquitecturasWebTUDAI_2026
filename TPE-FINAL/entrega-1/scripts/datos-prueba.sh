#!/usr/bin/env bash
# Carga datos de prueba a traves del API Gateway: 3 lugares y 6 eventos (5 publicados y 1 en borrador).
# Requisitos: curl y jq. Los servicios tienen que estar levantados (ver README).
#
# Uso: ./scripts/datos-prueba.sh [url-del-gateway]
set -euo pipefail

GW="${1:-http://localhost:8080}/api"
ORG1=10   # organizador "Rock Producciones"
ORG2=11   # organizador "Teatro Municipal"
ADMIN=1

post() { # post <ruta> <usuario> <rol> <json>
  curl -sf -X POST "$GW$1" -H "Content-Type: application/json" -H "X-Usuario-Id: $2" -H "X-Rol: $3" -d "$4"
}
put() {
  curl -sf -X PUT "$GW$1" -H "Content-Type: application/json" -H "X-Usuario-Id: $2" -H "X-Rol: $3" -d "$4"
}

# fecha dentro de N dias a las 21hs, en formato ISO
fecha() { date -d "+$1 days" +%Y-%m-%dT21:00:00; }

echo "== Lugares"
ESTADIO=$(post /lugares $ADMIN ADMIN '{
  "nombre": "Estadio Único Diego Armando Maradona", "direccion": "Av. 25 y 32", "ciudad": "La Plata",
  "latitud": -34.9137, "longitud": -57.9891,
  "sectores": [
    {"nombre": "Campo", "tipo": "NO_NUMERADO", "capacidad": 200},
    {"nombre": "Platea Norte", "tipo": "NUMERADO", "filas": 5, "asientosPorFila": 10},
    {"nombre": "Platea Sur", "tipo": "NUMERADO", "filas": 5, "asientosPorFila": 10}
  ]}' | jq -r .id)
TEATRO=$(post /lugares $ADMIN ADMIN '{
  "nombre": "Teatro del Fuerte", "direccion": "Gral. Rodríguez 551", "ciudad": "Tandil",
  "latitud": -37.3264, "longitud": -59.1370,
  "sectores": [
    {"nombre": "Platea", "tipo": "NUMERADO", "filas": 8, "asientosPorFila": 12},
    {"nombre": "Pullman", "tipo": "NUMERADO", "filas": 3, "asientosPorFila": 10}
  ]}' | jq -r .id)
SALA=$(post /lugares $ADMIN ADMIN '{
  "nombre": "Club Atlético Independiente - Microestadio", "direccion": "Av. Colón 1400", "ciudad": "Tandil",
  "latitud": -37.3190, "longitud": -59.1450,
  "sectores": [
    {"nombre": "General", "tipo": "NO_NUMERADO", "capacidad": 5}
  ]}' | jq -r .id)
echo "   Estadio=$ESTADIO Teatro=$TEATRO Sala=$SALA"

sectores() { curl -sf "$GW/lugares/$1" | jq -r '.sectores[].id'; }

# crear_evento <org> <json> <precio1> <precio2> ... -> publica el evento y devuelve el id
crear_evento() {
  local org=$1 json=$2; shift 2
  local id lugar precios="" i=0
  id=$(post /eventos "$org" ORGANIZADOR "$json" | jq -r .id)
  lugar=$(echo "$json" | jq -r .lugarId)
  local precios_arr=("$@")
  for s in $(sectores "$lugar"); do
    precios+="{\"sectorId\": $s, \"precio\": ${precios_arr[$i]}},"
    i=$((i + 1))
  done
  put "/eventos/$id/precios" "$org" ORGANIZADOR "{\"precios\": [${precios%,}]}" > /dev/null
  echo "$id"
}

publicar() { post "/eventos/$1/publicar" "$2" ORGANIZADOR '' > /dev/null; }

echo "== Eventos"
E1=$(crear_evento $ORG1 "{\"nombre\": \"Los Piojos - Ritual 2026\", \"descripcion\": \"La vuelta de Los Piojos al Estadio Único\",
  \"tipo\": \"RECITAL\", \"participantes\": [\"Los Piojos\"], \"fechaHora\": \"$(fecha 45)\", \"lugarId\": $ESTADIO,
  \"altaDemanda\": true, \"limiteEntradasPorCliente\": 4}" 60000 95000 90000)
publicar "$E1" $ORG1

E2=$(crear_evento $ORG1 "{\"nombre\": \"Festival Rock en el Bosque\", \"descripcion\": \"Festival con La Renga, Divididos y bandas invitadas\",
  \"tipo\": \"FESTIVAL\", \"participantes\": [\"La Renga\", \"Divididos\", \"Los Piojos\"], \"fechaHora\": \"$(fecha 60)\",
  \"lugarId\": $ESTADIO, \"limiteEntradasPorCliente\": 6}" 45000 70000 70000)
publicar "$E2" $ORG1

E3=$(crear_evento $ORG2 "{\"nombre\": \"Hamlet\", \"descripcion\": \"Clásico de Shakespeare por la Comedia de Tandil\",
  \"tipo\": \"TEATRO\", \"participantes\": [\"Comedia de Tandil\"], \"fechaHora\": \"$(fecha 10)\", \"lugarId\": $TEATRO}" 18000 12000)
publicar "$E3" $ORG2

E4=$(crear_evento $ORG2 "{\"nombre\": \"Charla: Microservicios en la práctica\", \"descripcion\": \"Arquitectura de software para sistemas de alta demanda\",
  \"tipo\": \"CONFERENCIA\", \"participantes\": [\"Juan Pérez\", \"Ana Gómez\"], \"fechaHora\": \"$(fecha 20)\", \"lugarId\": $TEATRO}" 5000 3000)
publicar "$E4" $ORG2

# evento chico (5 lugares) para probar que se agota y la venta del ultimo lugar
E5=$(crear_evento $ORG1 "{\"nombre\": \"Show acústico íntimo\", \"descripcion\": \"Solo 5 lugares\",
  \"tipo\": \"RECITAL\", \"participantes\": [\"Solista Invitado\"], \"fechaHora\": \"$(fecha 5)\", \"lugarId\": $SALA}" 10000)
publicar "$E5" $ORG1

# queda en borrador: no tiene que aparecer en las busquedas
E6=$(crear_evento $ORG2 "{\"nombre\": \"Obra en preparación\", \"descripcion\": \"Todavía no publicada\",
  \"tipo\": \"TEATRO\", \"participantes\": [], \"fechaHora\": \"$(fecha 90)\", \"lugarId\": $TEATRO}" 15000 10000)

echo "   Publicados: $E1 $E2 $E3 $E4 $E5 - Borrador: $E6"
echo "Listo. Probá: curl '$GW/eventos?ciudad=Tandil&orden=precio'"
