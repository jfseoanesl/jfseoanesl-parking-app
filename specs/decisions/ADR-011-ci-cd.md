# ADR-011 — CI/CD con GitHub Actions y publicación en Docker Hub

**Estado:** Aceptado (2026-10-20), a pedido del usuario.

## Contexto

Las imágenes de backend y frontend se construían solo en local (`docker compose ... --build`). El usuario pidió un flujo de CI/CD en GitHub Actions que publique ambas imágenes en Docker Hub.

Decisiones del usuario:
- Se publica con cada push a `master` y con los tags `vX.Y.Z`.
- Antes de publicar deben pasar las pruebas del backend sobre SQLite y sobre MySQL 8.4, y las del frontend.
- Los nombres de las imágenes son `jfseoanesl/parking-backend` y `jfseoanesl/parking-frontend`.

## Decisión

- **Workflow** `.github/workflows/ci-cd.yml`. Se dispara con `push` a `master` y con tags `v*.*.*`, con `pull_request` hacia `master` y de forma manual.
  - Con `concurrency`, un push nuevo cancela la corrida anterior de la misma rama; nunca la de un tag.
  - El permiso del `GITHUB_TOKEN` es solo `contents: read`.
- **Tres jobs de pruebas en paralelo**, que reutilizan exactamente lo que se corre en local:
  1. `mvn -B -q test` (SQLite en memoria).
  2. `scripts/verify-mysql.sh`, que levanta un MySQL 8.4 desechable con Docker en el runner.
  3. Frontend: `npm ci`, `tsc -b`, `vitest run` y `npm run build`.
  
  Si fallan, se suben los reportes de Surefire como artefacto.
- **Job de imágenes** (matriz `backend`/`frontend`), que depende de los tres anteriores:
  - Usa `docker/metadata-action` para los tags y las etiquetas OCI y `docker/build-push-action` con caché de GitHub Actions por componente.
  - Los Dockerfiles multi-etapa existentes no cambian: compilan dentro de la imagen.
  - Plataforma `linux/amd64`.
- **Tags publicados:**
  - `master`: `latest` y `sha-<commit>`.
  - `vX.Y.Z`: `X.Y.Z`, `X.Y` y `latest`.
  - Ejecución manual: `sha-<commit>`.
  - Pull requests: solo se construye, sin login ni publicación. Así se validan los Dockerfiles, incluso desde forks sin acceso a secretos.
- **Cuenta e imágenes:** el usuario y namespace de Docker Hub es `jfseoanesl`, fijado en el workflow (`env.DOCKERHUB_NAMESPACE`). Como no es secreto, aparece legible en los logs. Las imágenes son `jfseoanesl/parking-backend` y `jfseoanesl/parking-frontend`.
- **Credenciales:** solo el secreto `DOCKERHUB_TOKEN`, un token de acceso de Docker Hub limitado a *Read & Write*. Nunca se usa la contraseña.
- **Despliegue desde Docker Hub:** `docker-compose.hub.yml` se combina con `docker-compose.prod.yml`, fija `image: ${DOCKERHUB_NAMESPACE:-jfseoanesl}/parking-*:${IMAGE_TAG:-latest}` y `pull_policy: always`, y se usa con `up --no-build`. El frontend sigue esperando que el servicio del backend se llame `backend` (`nginx.conf`).

## Alternativas descartadas

- **Compilar el jar y el `dist` en el runner y copiarlos a imágenes "delgadas":** duplicaría la lógica de build de los Dockerfiles. La caché de capas por componente mantiene el tiempo bajo.
- **E2E en CI:** quedan fuera por decisión del usuario. Con SQLite y varios navegadores en paralelo son sensibles a la latencia (ver la bitácora). Se pueden agregar después como otro job que levante `docker compose` y corra Playwright con menos workers.
- **Multi-arquitectura (`linux/arm64`):** con emulación QEMU, el build de Maven tarda varias veces más. Basta con agregarla a `platforms` si se despliega en ARM.

## Consecuencias

- Nada se publica si falla alguna de las pruebas.
- El workflow queda en el repositorio, pero **se ejecuta en GitHub**: requiere subir la rama o fusionarla en `master` y configurar los dos secretos.
- Verificación local del workflow: sintaxis YAML y `actionlint`. La primera corrida real ocurre al subirlo.
