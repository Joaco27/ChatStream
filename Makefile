.PHONY: all build server client clientN clean levantar-servidores bajar-servidores levantar-clientes bajar-clientes

# Compila el proyecto con Maven
build:
	mvn clean compile

# Ejecuta el servidor localmente
server:
	mvn -q exec:java -Dexec.mainClass="ar.edu.unlp.info.oo2.ChatStream.ChatServer" -Dexec.cleanupDaemonThreads=false

# Ejecuta un cliente real localmente
client:
	mvn -q exec:java -Dexec.mainClass="ar.edu.unlp.info.oo2.ChatStream.ChatClient" -Dexec.cleanupDaemonThreads=false

# Ejecuta un cliente que envia N mensajes localmente
clientN:
	mvn -q exec:java -Dexec.mainClass="ar.edu.unlp.info.oo2.ChatStream.ChatClientN" -Dexec.cleanupDaemonThreads=false

# Limpia la compilación local
clean:
	mvn clean

# Levanta servidores y Redis con Docker Compose vía script
levantar-servidores:
	./levantar-servidores.sh

# Baja servidores y Redis vía script
bajar-servidores:
	./bajar-servidores.sh

# Levanta clientes vía script
levantar-clientes:
	./levantar-clientes.sh $(N) $(MSGS)
	
# Baja clientes (informativo, no hay contenedores persistentes)
bajar-clientes:
	./bajar-clientes.sh