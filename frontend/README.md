# Parqueadero — Frontend

PWA en React 19 + TypeScript + Vite 6 + Tailwind CSS 4. La documentación general del proyecto (funcionalidades, despliegue, CI/CD) está en el [README principal](../README.md).

## Comandos

| Comando | Qué hace |
|---|---|
| `npm install` | Instala las dependencias (Node 22) |
| `npm run dev` | Servidor de desarrollo en http://localhost:5173; reenvía `/api/**` al backend en `:8080` |
| `npm run build` | Build de producción en `dist/`, con el service worker de la PWA (Workbox) |
| `npm run test` | Pruebas unitarias y de componentes (Vitest + Testing Library) |
| `npx tsc -b` | Verificación de tipos |
| `npm run test:e2e` | E2E y responsive con Playwright (desktop y Pixel 7). Requiere backend y frontend corriendo |

Variables de entorno de los E2E:
- `E2E_BASE_URL`: otro stack, por ejemplo `http://localhost:8081` (Docker).
- `E2E_ADMIN_USERNAME` / `E2E_ADMIN_PASSWORD`: administrador distinto de `admin/admin123`.
- `E2E_BROWSER_CHANNEL=chrome`: usa el Chrome instalado en lugar del Chromium de Playwright.

## Estructura

```text
src/
├── api/          Cliente HTTP (sesión + CSRF) y tipos de la API
├── auth/         Contexto de autenticación
├── components/   Componentes reutilizables (Button, Card, Badges, ConfirmDialog, Toast, ReceiptLayout…)
│   └── reports/  Reporte de mensualidades
├── layout/       Shell, barra lateral, barra inferior móvil, navegación por rol
├── lib/          Utilidades (formato, períodos, mensualidades, respaldo…) con sus pruebas
└── pages/        Pantallas
e2e/              Pruebas Playwright
nginx.conf        Servidor y proxy de /api en la imagen Docker
```

Diseño y navegación: [`specs/09-ux-specification.md`](../specs/09-ux-specification.md).
