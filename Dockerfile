# Etapa 1: build con Maven y copia dependencias
FROM maven:3.9.2-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Compila y copia las dependencias al directorio target/dependency
RUN mvn clean compile dependency:copy-dependencies

# Etapa 2: runtime con openjdk
FROM openjdk:17-jdk-alpine

WORKDIR /app

# Copiamos clases compiladas y dependencias
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency

# Seteamos CLASSPATH con clases y todas las dependencias
ENV CLASSPATH=./classes:./dependency/*

# ENTRYPOINT genérico para ejecutar cualquier clase pasando argumentos (ejemplo puerto)
ENTRYPOINT ["java", "-cp", "./classes:./dependency/*"]
