#!/usr/bin/env bash
# Funciones comunes de los scripts: login contra ms-usuarios (a traves del gateway) y llamadas con JWT.
# Usuarios precargados (ms-usuarios, DatosIniciales):
#   admin@ticketya.com / admin123 (ADMIN)            organizador@ticketya.com / organizador123 (ORGANIZADOR)
#   organizador2@ticketya.com / organizador123        cliente@ticketya.com / cliente123 (CLIENTE)
#   cliente2@ticketya.com / cliente123                acceso@ticketya.com / acceso123 (ACCESO)

GW="${GW:-http://localhost:8080}/api"

# login <email> <password> -> imprime el token
login() {
  curl -sf -X POST "$GW/auth/login" -H "Content-Type: application/json" \
    -d "{\"email\": \"$1\", \"password\": \"$2\"}" | jq -r .token
}

# registrar <email> <nombre> -> crea un CLIENTE (si ya existe hace login) e imprime el token
registrar() {
  curl -s -o /dev/null -X POST "$GW/auth/registro" -H "Content-Type: application/json" \
    -d "{\"email\": \"$1\", \"nombre\": \"$2\", \"password\": \"cliente123\"}"
  login "$1" cliente123
}

# api <metodo> <ruta> <token> [json]
api() {
  if [ -n "${4:-}" ]; then
    curl -sf -X "$1" "$GW$2" -H "Authorization: Bearer $3" -H "Content-Type: application/json" -d "$4"
  else
    curl -sf -X "$1" "$GW$2" -H "Authorization: Bearer $3"
  fi
}
