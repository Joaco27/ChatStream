#!/bin/bash

echo "Bajando servidores"
docker-compose -f docker-compose-servers.yml down --remove-orphans

echo "Contenedores de servidores bajados."
