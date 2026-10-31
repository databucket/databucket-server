import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    build: {
        outDir: 'build',
        // the Gradle build turns it off where lightningcss can't load (Alpine), see build.gradle
        cssMinify: process.env.DATABUCKET_CSS_MINIFY !== 'false',
    },
    server: {
        port: 3000,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            }
        }
    }
})
