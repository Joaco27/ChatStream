#!/bin/bash

echo "Bajando servidores y Redis..."
docker-compose -f docker-compose-servers.yml down --remove-orphans

echo "Contenedores de servidores y Redis bajados."
