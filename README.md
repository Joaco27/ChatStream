** Proyecto desarrollado por Joaquin Tartaruga y Manuel Rubiano para la materia PDyTR

El proyecto cuenta con un makefile que contiene varios comandos necesarios para la ejecución:

- Compilar el proyecto
    make build
  
- Levantar el contenedor de servidores
    make levantar-servidores
  
- Ejecutar clientes
    make levantar-clientes
    Opcionalmente, tiene 2 argumentos:
      N: cantidad de clientes
      MSGS: cantidad de mensajes por cliente
    Ej para ejecutar 2 clientes que manden 2 mensajes cada uno:
    make levantar-clientes N=2 MSGS=2
