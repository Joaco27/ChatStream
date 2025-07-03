#!/bin/bash

N=${1:-1}                 # Número de clientes a lanzar
MSGS=${2:-5}              # Mensajes por cliente (default 5)
USER=${3:-usuario}        # Nombre de usuario base (default "usuario")

echo "Creando red Docker backend-net si no existe..."
docker network inspect backend-net >/dev/null 2>&1 || docker network create backend-net

echo "Reconstruyendo imagen sin cache..."
docker-compose -f docker-compose-clients.yml build --no-cache

echo "Lanzando $N cliente(s)..."

for i in $(seq 1 $N); do
  export MSG_COUNT=$MSGS
  export USERNAME="${USER}${i}"  # Diferencia cada cliente por número
  docker-compose -f docker-compose-clients.yml run --rm -T chatclient &
  sleep 1
done

wait
echo "Clientes finalizados."

# Calcular promedio general

if [[ -f resultados/resultados.txt ]]; then
  echo "Calculando promedio general..."

  total=0
  count=0

  while IFS= read -r line; do
    # Buscar líneas con 'PROMEDIO:' y extraer el valor numérico
    if [[ "$line" == *"PROMEDIO:"* ]]; then
      valor=$(echo "$line" | cut -d':' -f2 | tr -d 'a-zA-Z ' | tr -d 'ms')
      if [[ "$valor" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
        total=$(echo "$total + $valor" | bc)
        count=$((count + 1))
      fi
    fi
  done < resultados/resultados.txt

  if [[ "$count" -gt 0 ]]; then
    promedio=$(echo "scale=2; $total / $count" | bc)
    echo "Promedio general de RTTs: $promedio ms ($count Clientes, $MSGS Mensajes por Cliente)"
  else
    echo "No se encontraron líneas válidas con PROMEDIO en el archivo."
  fi
else
  echo "Archivo resultados/resultados.txt no encontrado."
fi
