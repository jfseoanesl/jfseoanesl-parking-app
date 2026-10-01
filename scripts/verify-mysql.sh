#!/usr/bin/env bash
# Fase 12 - Preparacion MySQL. Ejecuta TODA la suite del backend (la misma que corre sobre SQLite)
# contra un MySQL 8.4 real y desechable, sin cambiar codigo ni configuracion: solo se sobrescribe el
# datasource con variables de entorno (tienen prioridad sobre application-test.yml). Si pasa, el
# dominio funciona igual en ambos motores. Ver specs/decisions/ADR-006-migracion-mysql.md.
#
# Uso (desde la raiz del repo, requiere Docker, Java 21 y Maven):
#   bash scripts/verify-mysql.sh
set -euo pipefail

CONTAINER=parking-mysql-verify
PORT=${VERIFY_MYSQL_PORT:-13307}
PASS=verify-$RANDOM$RANDOM

cleanup() { docker rm -f "$CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup

echo "==> Levantando MySQL 8.4 desechable en el puerto $PORT"
# Buffer pool pequeno y sin performance_schema: basta para las pruebas y cabe en equipos con poca RAM.
docker run -d --name "$CONTAINER" -p "$PORT:3306" \
  -e MYSQL_ROOT_PASSWORD="$PASS" -e MYSQL_DATABASE=parking_test \
  mysql:8.4 --innodb-buffer-pool-size=64M --performance-schema=OFF >/dev/null

for _ in $(seq 1 90); do
  docker exec "$CONTAINER" mysqladmin ping -h 127.0.0.1 -uroot -p"$PASS" --silent >/dev/null 2>&1 && break
  sleep 2
done

echo "==> Ejecutando la suite del backend contra MySQL"
cd "$(dirname "$0")/../backend"
rm -rf target/surefire-reports
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:$PORT/parking_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC" \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD="$PASS" \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect \
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5 \
MAVEN_OPTS="${MAVEN_OPTS:--Xmx384m}" \
  mvn -q test -DargLine=-Xmx512m

echo "==> Migraciones aplicadas en MySQL:"
docker exec "$CONTAINER" mysql -uroot -p"$PASS" -N parking_test \
  -e "SELECT CONCAT('   V', version, ' ', description) FROM flyway_schema_history ORDER BY installed_rank;" 2>/dev/null
echo "==> OK: la suite completa pasa sobre MySQL 8.4"
