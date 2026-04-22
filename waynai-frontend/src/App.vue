<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppNavigation from '@/components/AppNavigation.vue'
import { useThemeStore } from '@/stores/theme'
import { onMounted } from 'vue'

const themeStore = useThemeStore()

onMounted(() => {
  themeStore.initTheme()
  themeStore.watchSystemTheme()
})
</script>

<template>
  <div id="app">
    <AppNavigation />
    
    <main class="main">
      <RouterView />
    </main>

    <footer class="footer">
      <div class="footer-container">
        <div class="footer-brand">
          <span class="footer-logo">wayn<em>ai</em></span>
          <span class="footer-dot" aria-hidden="true"></span>
        </div>
        <p class="footer-tag">Every Way, Your Way — with WaynAI</p>
        <p class="footer-copy">&copy; 2026 WaynAI · AI 기반 여행 어시스턴트</p>
      </div>
    </footer>
    
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--m3-background);
  color: var(--m3-on-background);
  transition: background var(--m3-motion-medium), color var(--m3-motion-medium);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.footer {
  background: var(--wa-ocean);
  color: var(--wa-cream);
  padding: 2.5rem 0 2rem;
  margin-top: auto;
  transition: background var(--m3-motion-medium);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}

.footer-brand {
  display: inline-flex;
  align-items: baseline;
  gap: 0.125rem;
  font-family: var(--wa-font-serif);
  font-size: 1.625rem;
  font-weight: 500;
  color: var(--wa-cream);
}
.footer-logo em {
  color: var(--wa-amber);
  font-style: italic;
  font-weight: 500;
}
.footer-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--wa-amber);
  margin-left: 4px;
  transform: translateY(-2px);
}

.footer-tag {
  font-family: var(--wa-font-serif);
  font-style: italic;
  font-size: 1.125rem;
  color: var(--wa-sand);
  margin: 0;
}
.footer-copy {
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: color-mix(in srgb, var(--wa-cream) 70%, transparent);
  letter-spacing: 0.04em;
  margin: 0;
}
</style>
