<template>
  <header class="nav-wrap">
    <div class="nav-inner">
      <RouterLink to="/" class="brand">
        <span class="brand-main">wayn</span><span class="brand-accent">ai</span>
        <span class="brand-dot" aria-hidden="true"></span>
      </RouterLink>

      <nav class="nav-links" aria-label="주요 메뉴">
        <RouterLink to="/" class="nav-link">Home</RouterLink>
        <RouterLink to="/travel-plan" class="nav-link">여행 계획</RouterLink>
        <RouterLink to="/tourist-info" class="nav-link">관광지</RouterLink>
        <RouterLink to="/about" class="nav-link">About</RouterLink>
      </nav>

      <div class="nav-actions">
        <button
          @click="toggleTheme"
          class="theme-toggle"
          :title="isDarkMode ? '라이트 모드로 변경' : '다크 모드로 변경'"
        >
          <span class="theme-icon" aria-hidden="true">{{ isDarkMode ? '☀︎' : '☾' }}</span>
        </button>
        <RouterLink to="/travel-plan" class="cta-pill">여행 계획 시작</RouterLink>
      </div>

      <button class="nav-toggle" @click="toggleMobileMenu" aria-label="메뉴 열기">
        <span></span>
        <span></span>
        <span></span>
      </button>
    </div>

    <div v-if="isMobileMenuOpen" class="mobile-menu">
      <RouterLink to="/" class="mobile-link" @click="closeMobileMenu">Home</RouterLink>
      <RouterLink to="/travel-plan" class="mobile-link" @click="closeMobileMenu">여행 계획</RouterLink>
      <RouterLink to="/tourist-info" class="mobile-link" @click="closeMobileMenu">관광지 정보</RouterLink>
      <RouterLink to="/about" class="mobile-link" @click="closeMobileMenu">About</RouterLink>
      <button @click="toggleThemeAndClose" class="mobile-theme-toggle">
        <span class="theme-icon">{{ isDarkMode ? '☀︎' : '☾' }}</span>
        <span class="theme-text">{{ isDarkMode ? '라이트 모드' : '다크 모드' }}</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink } from 'vue-router';
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
.nav-wrap {
  position: sticky;
  top: 0;
  z-index: 100;
  background: color-mix(in srgb, var(--wa-cream) 92%, transparent);
  backdrop-filter: saturate(1.2) blur(12px);
  border-bottom: 1px solid color-mix(in srgb, var(--wa-sand) 60%, transparent);
  transition: background 200ms ease, border-color 200ms ease;
}

.nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.875rem 1.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.5rem;
  min-height: 70px;
}

.brand {
  position: relative;
  display: inline-flex;
  align-items: baseline;
  gap: 0.125rem;
  font-family: var(--wa-font-serif);
  font-size: 1.75rem;
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--wa-ocean);
  text-decoration: none;
  line-height: 1;
}
.brand-accent {
  font-style: italic;
  color: var(--wa-terra);
}
.brand-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--wa-amber);
  margin-left: 4px;
  transform: translateY(-2px);
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2rem;
}
.nav-link {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--wa-text-mid);
  text-decoration: none;
  transition: color 150ms ease;
  position: relative;
  padding: 0.25rem 0;
}
.nav-link:hover { color: var(--wa-ocean); }
.nav-link.router-link-exact-active {
  color: var(--wa-ocean);
}
.nav-link.router-link-exact-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -6px;
  height: 2px;
  background: var(--wa-terra);
  border-radius: 999px;
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.theme-toggle {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 70%, transparent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--wa-text-mid);
  cursor: pointer;
  transition: all 150ms ease;
}
.theme-toggle:hover {
  background: var(--wa-sand);
  color: var(--wa-ocean);
  transform: translateY(-1px);
}
.theme-icon { font-size: 1rem; line-height: 1; }

.cta-pill {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.625rem 1.25rem;
  border-radius: 999px;
  background: var(--wa-ocean);
  color: var(--wa-cream);
  font-family: var(--wa-font-sans);
  font-size: 0.875rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  text-decoration: none;
  transition: background 150ms ease, transform 150ms ease, box-shadow 200ms ease;
  box-shadow: 0 8px 20px -10px color-mix(in srgb, var(--wa-ocean) 55%, transparent);
}
.cta-pill:hover {
  background: var(--wa-dusk);
  transform: translateY(-1px);
  box-shadow: 0 12px 24px -10px color-mix(in srgb, var(--wa-ocean) 65%, transparent);
}

.nav-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  border: none;
  background: transparent;
  transition: background 150ms ease;
}
.nav-toggle:hover { background: var(--wa-sand); }
.nav-toggle span {
  width: 20px;
  height: 2px;
  border-radius: 2px;
  background: var(--wa-ocean);
}

.mobile-menu {
  display: none;
  flex-direction: column;
  padding: 0.75rem 1.25rem 1.25rem;
  background: var(--wa-cream);
  border-top: 1px solid color-mix(in srgb, var(--wa-sand) 70%, transparent);
  gap: 0.25rem;
}
.mobile-link {
  padding: 0.75rem 1rem;
  border-radius: 12px;
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-dark);
  text-decoration: none;
  transition: background 150ms ease;
}
.mobile-link:hover { background: var(--wa-sand); }
.mobile-link.router-link-exact-active {
  background: var(--wa-ocean);
  color: var(--wa-cream);
}
.mobile-theme-toggle {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: transparent;
  border: none;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  cursor: pointer;
  color: var(--wa-text-dark);
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
}
.mobile-theme-toggle:hover { background: var(--wa-sand); }
.theme-text { font-size: 0.9375rem; }

@media (max-width: 900px) {
  .nav-links, .nav-actions { display: none; }
  .nav-toggle { display: inline-flex; }
  .mobile-menu { display: flex; }
  .nav-inner { padding: 0.75rem 1rem; min-height: 60px; }
}

.dark .nav-wrap {
  background: color-mix(in srgb, var(--wa-ocean) 85%, transparent);
  border-bottom-color: color-mix(in srgb, var(--wa-mist) 20%, transparent);
}
.dark .brand { color: var(--wa-cream); }
.dark .brand-accent { color: var(--wa-amber); }
.dark .nav-link { color: var(--wa-mist); }
.dark .nav-link:hover, .dark .nav-link.router-link-exact-active { color: var(--wa-cream); }
.dark .theme-toggle { border-color: color-mix(in srgb, var(--wa-mist) 30%, transparent); color: var(--wa-mist); }
.dark .theme-toggle:hover { background: color-mix(in srgb, var(--wa-cream) 8%, transparent); color: var(--wa-cream); }
</style>
