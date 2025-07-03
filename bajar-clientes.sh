#!/bin/bash

echo "Bajando clientes..."
docker-compose -f docker-compose-clients.yml down --remove-orphans

echo "Contenedores de clientes bajados."
