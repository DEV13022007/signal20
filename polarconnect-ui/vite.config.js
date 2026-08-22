import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true,
      },
    },
  },
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      injectRegister: 'auto',
      includeAssets: ['favicon.svg', 'icons.svg'],
      manifest: {
        name: 'PolarConnect',
        short_name: 'PolarConnect',
        description: 'Offline-first management platform for Antarctic research stations',
        theme_color: '#0b1930',
        background_color: '#0b1930',
        display: 'standalone',
        start_url: '/',
        icons: [
          { src: '/pwa-icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any' },
          { src: '/pwa-icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'maskable' },
        ],
      },
      workbox: {
        // App shell + static assets: precached so the dashboard boots with no network at
        // all. API responses are deliberately NOT cached at the service-worker level:
        // Dexie (see src/db) is the single source of truth for offline reads, and a SW
        // cache keyed only by URL — not by Authorization header — would serve one user's
        // scoped response to the next user/role sharing the same browser.
        globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
        navigateFallback: '/index.html',
      },
      devOptions: {
        enabled: true,
        type: 'module',
      },
    }),
  ],
})
