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
        <router-link to="/" class="nav-link">{{ t('nav.home') }}</router-link>
        <router-link to="/travel-plan" class="nav-link">{{ t('nav.travel_plan') }}</router-link>
        <router-link to="/tourist-info" class="nav-link">{{ t('nav.tourist_info') }}</router-link>
        <router-link to="/recommendations" class="nav-link">{{ t('nav.recommendations') }}</router-link>
        <router-link to="/about" class="nav-link">{{ t('nav.about') }}</router-link>
        
        <!-- 다크모드 토글 -->
        <button @click="toggleTheme" class="theme-toggle" :title="isDarkMode ? '라이트 모드로 변경' : '다크 모드로 변경'">
          <span v-if="isDarkMode" class="theme-icon">☀️</span>
          <span v-else class="theme-icon">🌙</span>
        </button>
        
        <!-- 언어 설정 -->
        <div class="language-selector">
          <select v-model="currentLanguage" @change="handleLanguageChange" class="language-select">
            <option v-for="lang in languages" :key="lang.code" :value="lang.code">
              {{ lang.flag }} {{ lang.name }}
            </option>
          </select>
        </div>
      </nav>
      
      <div class="nav-toggle" @click="toggleMobileMenu">
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
    
    <!-- 모바일 메뉴 -->
    <div v-if="isMobileMenuOpen" class="mobile-menu">
      <router-link to="/" class="mobile-nav-link" @click="closeMobileMenu">홈</router-link>
      <router-link to="/travel-plan" class="mobile-nav-link" @click="closeMobileMenu">여행 계획</router-link>
      <router-link to="/tourist-info" class="mobile-nav-link" @click="closeMobileMenu">관광 정보</router-link>
      <router-link to="/recommendations" class="mobile-nav-link" @click="closeMobileMenu">추천</router-link>
      <router-link to="/about" class="mobile-nav-link" @click="closeMobileMenu">소개</router-link>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useThemeStore } from '@/stores/theme';
import { useLanguageStore } from '@/stores/language';

const isMobileMenuOpen = ref(false);
const themeStore = useThemeStore();
const languageStore = useLanguageStore();

const { isDarkMode, toggleTheme } = themeStore;
const { currentLanguage, languages, changeLanguage, t } = languageStore;

const toggleMobileMenu = () => {
  isMobileMenuOpen.value = !isMobileMenuOpen.value;
};

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false;
};

const handleLanguageChange = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  languageStore.changeLanguage(target.value);
};
</script>

<style scoped>
.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  position: sticky;
  top: 0;
  z-index: 100;
  transition: background 0.3s ease;
}

/* 다크모드에서 헤더 배경 */
.dark .header {
  background: rgba(15, 23, 42, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.logo-icon {
  color: #1e3c72;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s ease;
}

/* 다크모드에서 로고 아이콘 */
.dark .logo-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.logo-icon svg {
  color: white;
}

.logo-text {
  font-size: 1.5rem;
  font-weight: 700;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  transition: background 0.3s ease;
}

/* 다크모드에서 로고 텍스트 */
.dark .logo-text {
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.tagline {
  font-size: 0.875rem;
  color: #6b7280;
  margin: 0;
  font-weight: 500;
  transition: color 0.3s ease;
}

/* 다크모드에서 태그라인 */
.dark .tagline {
  color: #9ca3af;
}

.nav {
  display: flex;
  gap: 2rem;
}

.nav-link {
  text-decoration: none;
  color: #374151;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
}

.nav-link:hover {
  color: #1e3c72;
  background: rgba(30, 60, 114, 0.1);
}

.nav-link.router-link-active {
  color: #1e3c72;
  background: rgba(30, 60, 114, 0.1);
  font-weight: 600;
}

/* 다크모드에서 네비게이션 링크 */
.dark .nav-link {
  color: #e2e8f0;
}

.dark .nav-link:hover {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.1);
}

.dark .nav-link.router-link-active {
  color: #60a5fa;
  background: rgba(96, 165, 250, 0.1);
}

.language-selector {
  margin-left: 1rem;
}

.language-select {
  padding: 0.5rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  font-size: 0.875rem;
  cursor: pointer;
  outline: none;
  transition: border-color 0.3s ease, background 0.3s ease, color 0.3s ease;
}

.language-select:focus {
  border-color: #667eea;
}

/* 다크모드에서 언어 선택 */
.dark .language-select {
  background: #1e293b;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .language-select:focus {
  border-color: #60a5fa;
}

.theme-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 6px;
  transition: background-color 0.3s ease;
  margin-left: 1rem;
}

.theme-toggle:hover {
  background: rgba(102, 126, 234, 0.1);
}

.theme-icon {
  font-size: 1.2rem;
}

.nav-toggle {
  display: none;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
  padding: 0.5rem;
}

.nav-toggle span {
  width: 25px;
  height: 3px;
  background: #2c3e50;
  transition: all 0.3s ease;
}

.mobile-menu {
  display: none;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.2);
  padding: 1rem 2rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 모바일 메뉴 */
.dark .mobile-menu {
  background: rgba(15, 23, 42, 0.98);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.mobile-nav-link {
  color: #2c3e50;
  text-decoration: none;
  font-weight: 500;
  padding: 1rem 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  transition: color 0.3s ease;
}

.mobile-nav-link:hover,
.mobile-nav-link.router-link-active {
  color: #667eea;
}

/* 다크모드에서 모바일 네비게이션 링크 */
.dark .mobile-nav-link {
  color: #e2e8f0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.dark .mobile-nav-link:hover,
.dark .mobile-nav-link.router-link-active {
  color: #60a5fa;
}

.mobile-nav-link:last-child {
  border-bottom: none;
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .header-container {
    padding: 1rem;
    flex-direction: column;
    gap: 1rem;
  }

  .logo-section {
    align-items: center;
  }

  .nav {
    display: none;
  }
  
  .nav-toggle {
    display: flex;
    position: absolute;
    top: 1rem;
    right: 1rem;
  }
  
  .mobile-menu {
    display: flex;
  }
}
</style> 