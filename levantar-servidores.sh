#!/bin/bash

echo "Creando red Docker backend-net si no existe..."
docker network inspect backend-net >/dev/null 2>&1 || docker network create backend-net

echo "Bajando contenedores anteriores (si existen)..."
docker-compose -f docker-compose-servers.yml down --remove-orphans

echo "Reconstruyendo imagenes sin cache..."
docker-compose -f docker-compose-servers.yml build --no-cache

echo "Levantando servidores y Redis..."
docker-compose -f docker-compose-servers.yml up -d

echo "SERVIDORES levantados."
