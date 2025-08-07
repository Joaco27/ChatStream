## 👥 Autores
Proyecto desarrollado por **Joaquin Tartaruga** y **Manuel Rubiano** para la materia *Programación Distribuida y de Tiempo Real (PDyTR)*.

## ⚙️ Makefile

El proyecto incluye un `Makefile` con varios comandos útiles para compilar y ejecutar el sistema:

- **Compilar el proyecto:**
  ```bash
  make build
  
- **Levantar contenedores de servidores:**
  ```bash
  make levantar-servidores
  
- **Ejecutar los clientes::**
  ```bash
  make levantar-clientes

Opcionalmente, el comando permite especificar dos argumentos:

N: cantidad de clientes a lanzar.

MSGS: cantidad de mensajes que enviará cada cliente.

Ejemplo para ejecutar 2 clientes que envíen 2 mensajes cada uno:

```bash
  make levantar-clientes N=2 MSGS=2
