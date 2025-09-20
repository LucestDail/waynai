import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface Language {
  code: string
  name: string
  flag: string
}

export const useLanguageStore = defineStore('language', () => {
  const currentLanguage = ref('ko')
  
  const languages: Language[] = [
    { code: 'ko', name: '한국어', flag: '🇰🇷' },
    { code: 'en', name: 'English', flag: '🇺🇸' },
    { code: 'ja', name: '日本語', flag: '🇯🇵' },
    { code: 'zh', name: '中文', flag: '🇨🇳' }
  ]

  // 언어 변경
  const changeLanguage = (languageCode: string) => {
    currentLanguage.value = languageCode
    localStorage.setItem('language', languageCode)
  }

  // 초기 언어 설정
  const initLanguage = () => {
    const savedLanguage = localStorage.getItem('language')
    if (savedLanguage) {
      currentLanguage.value = savedLanguage
    }
  }

  // 현재 언어 정보 가져오기
  const getCurrentLanguage = () => {
    return languages.find(lang => lang.code === currentLanguage.value) || languages[0]
  }

  // 번역 함수 (간단한 예시)
  const t = (key: string): string => {
    const translations: Record<string, Record<string, string>> = {
      'nav.home': {
        'ko': '홈',
        'en': 'Home',
        'ja': 'ホーム',
        'zh': '首页'
      },
      'nav.travel_plan': {
        'ko': '여행 계획',
        'en': 'Travel Plan',
        'ja': '旅行計画',
        'zh': '旅行计划'
      },
      'nav.tourist_info': {
        'ko': '관광 정보',
        'en': 'Tourist Info',
        'ja': '観光情報',
        'zh': '旅游信息'
      },
      'nav.recommendations': {
        'ko': '추천',
        'en': 'Recommendations',
        'ja': 'おすすめ',
        'zh': '推荐'
      },
      'nav.about': {
        'ko': '소개',
        'en': 'About',
        'ja': 'について',
        'zh': '关于'
      },
    }

    return translations[key]?.[currentLanguage.value] || key
  }

  return {
    currentLanguage,
    languages,
    changeLanguage,
    initLanguage,
    getCurrentLanguage,
    t
  }
}) 