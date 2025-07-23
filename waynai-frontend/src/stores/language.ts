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
      'travel_guide.title': {
        'ko': 'WaynAI 여행 가이드',
        'en': 'WaynAI Travel Guide',
        'ja': 'WaynAI 旅行ガイド',
        'zh': 'WaynAI 旅行指南'
      },
      'travel_guide.placeholder': {
        'ko': '여행에 대해 궁금한 점을 물어보세요...',
        'en': 'Ask about your travel...',
        'ja': '旅行について質問してください...',
        'zh': '询问您的旅行问题...'
      },
      'travel_guide.send': {
        'ko': '전송',
        'en': 'Send',
        'ja': '送信',
        'zh': '发送'
      },
      'travel_guide.welcome': {
        'ko': '안녕하세요! WaynAI 여행 가이드입니다. 여행에 대해 궁금한 점이 있으시면 언제든 물어보세요!',
        'en': 'Hello! I\'m WaynAI Travel Guide. Feel free to ask me anything about your travel!',
        'ja': 'こんにちは！WaynAI旅行ガイドです。旅行について何でもお気軽にお聞きください！',
        'zh': '您好！我是WaynAI旅行指南。请随时询问您的旅行问题！'
      }
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