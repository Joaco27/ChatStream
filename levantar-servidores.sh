#!/bin/bash

# Crear archivo de mensajes compartidos vacío
rm -f mensajes/mensajes-compartidos.txt
touch mensajes/mensajes-compartidos.txt

echo "Creando red Docker backend-net si no existe..."
docker network inspect backend-net >/dev/null 2>&1 || docker network create backend-net

echo "Bajando contenedores anteriores (si existen)..."
docker-compose -f docker-compose-servers.yml down --remove-orphans

echo "Reconstruyendo imagenes sin cache..."
docker-compose -f docker-compose-servers.yml build --no-cache

echo "Levantando servidores..."
docker-compose -f docker-compose-servers.yml up -d

echo "SERVIDORES levantados."
