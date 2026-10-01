# syntax=docker/dockerfile:1
# Fase 11 - Contenedorizacion. Build multi-etapa: la imagen final no incluye Maven ni el JDK
# completo, solo el JRE necesario para ejecutar el jar ya compilado.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Copiar solo el pom primero permite reutilizar la cache de dependencias mientras no cambie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Perfil "development" (SQLite en archivo) escribe en ./data - ver application-development.yml.
# Usuario no-root (prompt.md - buenas practicas de seguridad de contenedores).
RUN addgroup -S parking && adduser -S parking -G parking \
    && mkdir -p /app/data && chown -R parking:parking /app
COPY --from=build /build/target/*.jar app.jar
USER parking
VOLUME ["/app/data"]
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
