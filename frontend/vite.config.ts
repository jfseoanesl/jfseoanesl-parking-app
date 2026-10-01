import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: 'Parqueadero',
        short_name: 'Parqueadero',
        description: 'Gestion de ingreso, salida y cobro de un parqueadero pequeno',
        lang: 'es',
        theme_color: '#1d4ed8',
        background_color: '#ffffff',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'icons/icon-512-maskable.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // 09-ux-specification.md / 06-architecture.md: cache-first solo para el shell estatico;
        // las llamadas a /api/** nunca se cachean (BR-019, offline transaccional fuera de alcance).
        navigateFallbackDenylist: [/^\/api\//],
        // Incluye las fuentes empaquetadas (woff2) para que el shell offline conserve la tipografia.
        globPatterns: ['**/*.{js,css,html,svg,png,woff2}'],
        runtimeCaching: [
          {
            urlPattern: /^\/api\/.*/,
            handler: 'NetworkOnly',
          },
        ],
      },
    }),
  ],
  server: {
    proxy: {
      // Evita CORS/cookies cross-site en desarrollo: el navegador ve todo como same-origin
      // (localhost:5173), Vite reenvia /api al backend (localhost:8080).
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: false,
      },
    },
  },
})
