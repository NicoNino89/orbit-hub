/// <reference types="vitest/config" />
/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Configuração do Vite e do Vitest
 * Objetivo: Definir plugins (React e Tailwind), o proxy da API em desenvolvimento e o ambiente de testes.
 * Decisões: Proxy de /api para o back-end local, sem CORS em desenvolvimento; testes em jsdom; cobertura
 *           V8 em lcov para o SonarCloud, sem contar main.tsx, declarações e a preparação dos testes.
 */
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Em desenvolvimento, /api vai para o back-end local (mvnw spring-boot:run ou docker compose).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/main.tsx', 'src/**/*.d.ts', 'src/test/**'],
    },
  },
})
