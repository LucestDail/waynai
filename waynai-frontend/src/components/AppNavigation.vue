<template>
  <header class="header">
    <div class="header-container">
      <div class="logo-section">
        <div class="logo">
          <div class="logo-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" fill="currentColor"/>
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
            </svg>
          </div>
          <span class="logo-text">WaynAI</span>
        </div>
        <p class="tagline">AI가 이끄는 당신만의 여행 길</p>
      </div>
      
      <nav class="nav">
        <!-- 다크모드 토글 -->
        <button @click="toggleTheme" class="theme-toggle" :title="isDarkMode ? '라이트 모드로 변경' : '다크 모드로 변경'">
          <span v-if="isDarkMode" class="theme-icon">🌅</span>
          <span v-else class="theme-icon">🌙</span>
        </button>
      </nav>
      
      <div class="nav-toggle" @click="toggleMobileMenu">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
    
    <!-- 모바일 메뉴 -->
    <div v-if="isMobileMenuOpen" class="mobile-menu">
      <!-- 모바일에서는 다크모드 토글만 표시 -->
      <button @click="toggleThemeAndClose" class="mobile-theme-toggle">
        <span v-if="isDarkMode" class="theme-icon">🌅</span>
        <span v-else class="theme-icon">🌙</span>
        <span class="theme-text">{{ isDarkMode ? '라이트 모드' : '다크 모드' }}</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useThemeStore } from '@/stores/theme';

const isMobileMenuOpen = ref(false);
const themeStore = useThemeStore();

const { isDarkMode, toggleTheme } = themeStore;

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
};

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false;
};

const toggleThemeAndClose = () => {
  toggleTheme();
  closeMobileMenu();
};
</script>

<style scoped>
/* M3 Top App Bar */
.header {
  background: var(--m3-surface-container-low);
  border-bottom: 1px solid var(--m3-outline-variant);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: background var(--m3-motion-medium), border-color var(--m3-motion-medium),
    box-shadow var(--m3-motion-medium);
}
.header:hover { box-shadow: var(--m3-elev-1); }

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.75rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  min-height: 64px;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.logo-icon {
  background: var(--m3-primary);
  color: var(--m3-on-primary);
  border-radius: var(--m3-shape-md);
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background var(--m3-motion-short), color var(--m3-motion-short);
}
.logo-icon svg { color: currentColor; }

.logo-text {
  font: var(--m3-title-large);
  color: var(--m3-on-surface);
  letter-spacing: -0.01em;
}

.tagline {
  font: var(--m3-label-medium);
  color: var(--m3-on-surface-variant);
  margin: 0;
  padding-left: 0.125rem;
  letter-spacing: 0.01em;
}

.nav {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

/* M3 Icon button (circular, 40px) */
.theme-toggle {
  background: transparent;
  border: none;
  cursor: pointer;
  width: 40px;
  height: 40px;
  border-radius: var(--m3-shape-full);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--m3-on-surface-variant);
  transition: background var(--m3-motion-short), color var(--m3-motion-short);
  position: relative;
}
.theme-toggle:hover {
  background: color-mix(in srgb, var(--m3-on-surface) 8%, transparent);
  color: var(--m3-on-surface);
}
.theme-toggle:active {
  background: color-mix(in srgb, var(--m3-on-surface) 12%, transparent);
}
.theme-icon { font-size: 1.125rem; line-height: 1; }

/* Hamburger (mobile) */
.nav-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  border-radius: var(--m3-shape-full);
  transition: background var(--m3-motion-short);
}
.nav-toggle:hover { background: color-mix(in srgb, var(--m3-on-surface) 8%, transparent); }
.nav-toggle span {
  width: 20px;
  height: 2px;
  border-radius: 2px;
  background: var(--m3-on-surface-variant);
  transition: background var(--m3-motion-short);
}

.mobile-menu {
  display: none;
  flex-direction: column;
  background: var(--m3-surface-container);
  border-top: 1px solid var(--m3-outline-variant);
  padding: 0.5rem 1rem;
  gap: 0.25rem;
}

.mobile-theme-toggle {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: transparent;
  border: none;
  color: var(--m3-on-surface);
  font: var(--m3-label-large);
  padding: 0.875rem 1rem;
  border-radius: var(--m3-shape-sm);
  cursor: pointer;
  width: 100%;
  text-align: left;
  transition: background var(--m3-motion-short);
}
.mobile-theme-toggle:hover {
  background: color-mix(in srgb, var(--m3-on-surface) 8%, transparent);
}
.theme-text { font: var(--m3-label-large); }

@media (max-width: 768px) {
  .header-container {
    padding: 0.5rem 1rem;
    min-height: 56px;
  }
  .logo-icon { width: 36px; height: 36px; }
  .logo-text { font: var(--m3-title-medium); }
  .tagline { display: none; }
  .nav-toggle { display: flex; }
  .mobile-menu { display: flex; }
}

@media (max-width: 480px) {
  .header-container { padding: 0.5rem 0.75rem; }
  .logo-icon { width: 32px; height: 32px; }
  .logo-icon svg { width: 20px; height: 20px; }
  .logo-text { font: var(--m3-title-medium); font-size: 1.0625rem; }
}
</style> 