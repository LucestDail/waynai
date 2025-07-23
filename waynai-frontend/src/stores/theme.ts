import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const isDarkMode = ref(false)

  // 초기 테마 설정 (로컬 스토리지에서 불러오기)
  const initTheme = () => {
    try {
      const savedTheme = localStorage.getItem('theme')
      if (savedTheme) {
        isDarkMode.value = savedTheme === 'dark'
      } else {
        // 시스템 설정 확인
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
        isDarkMode.value = prefersDark
      }
      applyTheme()
      console.log('Theme initialized:', isDarkMode.value ? 'dark' : 'light')
    } catch (error) {
      console.error('Failed to initialize theme:', error)
    }
  }

  // 테마 토글
  const toggleTheme = () => {
    isDarkMode.value = !isDarkMode.value
    localStorage.setItem('theme', isDarkMode.value ? 'dark' : 'light')
    applyTheme()
  }

  // 테마 적용
  const applyTheme = () => {
    try {
      const root = document.documentElement
      if (isDarkMode.value) {
        root.classList.add('dark')
        console.log('Dark mode applied')
      } else {
        root.classList.remove('dark')
        console.log('Light mode applied')
      }
    } catch (error) {
      console.error('Failed to apply theme:', error)
    }
  }

  // 시스템 테마 변경 감지
  const watchSystemTheme = () => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaQuery.addEventListener('change', (e) => {
      if (!localStorage.getItem('theme')) {
        isDarkMode.value = e.matches
        applyTheme()
      }
    })
  }

  return {
    isDarkMode,
    initTheme,
    toggleTheme,
    applyTheme,
    watchSystemTheme
  }
}) 