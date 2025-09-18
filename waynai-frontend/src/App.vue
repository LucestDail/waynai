<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppNavigation from '@/components/AppNavigation.vue'
import TravelGuideButton from '@/components/TravelGuideButton.vue'
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
    
    <!-- 여행 가이드 버튼 -->
    <TravelGuideButton />
  </div>
</template>

<style scoped>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  transition: background 0.3s ease;
}

.dark #app {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.footer {
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 1rem 0;
  margin-top: auto;
  transition: background 0.3s ease;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  text-align: center;
}

.footer p {
  color: rgba(255, 255, 255, 0.8);
  font-size: 0.875rem;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 푸터 */
.dark .footer {
  background: rgba(0, 0, 0, 0.3);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.dark .footer p {
  color: rgba(255, 255, 255, 0.6);
}
</style>
