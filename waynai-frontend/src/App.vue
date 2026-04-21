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
        <p>&copy; 2024 WaynAI. "Every Way, Your Way — with WaynAI"</p>
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
  background: var(--m3-surface-container-low);
  border-top: 1px solid var(--m3-outline-variant);
  padding: 1.25rem 0;
  margin-top: auto;
  transition: background var(--m3-motion-medium), border-color var(--m3-motion-medium);
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  text-align: center;
}

.footer p {
  color: var(--m3-on-surface-variant);
  font: var(--m3-body-small);
  margin: 0;
  letter-spacing: 0.02em;
}
</style>
