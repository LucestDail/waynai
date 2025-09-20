<template>
  <div class="waynai-info-container">
    <!-- WanynAi 소개 버튼 -->
    <button 
      class="info-button"
      @click="toggleInfo"
      :class="{ 'active': isInfoOpen }"
    >
      <div class="info-icon">🧠</div>
      <span class="info-text">WanynAi란?</span>
    </button>

    <!-- 소개 팝업 -->
    <div v-if="isInfoOpen" class="info-popup">
      <div class="popup-header">
        <h3>WanynAI 소개</h3>
        <button class="close-button" @click="toggleInfo">×</button>
      </div>
      
      <div class="popup-content">
        <div class="brand-intro">
          <div class="brand-logo">
            <div class="logo-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z" fill="currentColor"/>
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
              </svg>
            </div>
            <h4>WanynAI</h4>
          </div>
          <p class="brand-slogan">"당신의 길을 함께 찾는 여행 파트너"</p>
        </div>

        <div class="features-list">
          <div class="feature-item">
            <span class="feature-icon">🤖</span>
            <span class="feature-text">AI 여행 플래너</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🎯</span>
            <span class="feature-text">현지 맞춤 추천</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🗺️</span>
            <span class="feature-text">루트 최적화</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">💬</span>
            <span class="feature-text">대화형 여행 가이드</span>
          </div>
        </div>

        <div class="description">
          <p>WanynAI는 AI를 활용한 여행 계획 및 관광지 추천 서비스입니다. 
          사용자의 목적, 취향, 일정에 따라 맞춤형 여행을 설계해드립니다.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const isInfoOpen = ref(false);

const toggleInfo = () => {
  isInfoOpen.value = !isInfoOpen.value;
};
</script>

<style scoped>
.waynai-info-container {
  position: fixed;
  bottom: 2rem;
  right: 2rem;
  z-index: 1000;
}

.info-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 50px;
  padding: 1rem 1.5rem;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  font-weight: 600;
}

/* 다크모드에서 정보 버튼 */
.dark .info-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.4);
}

.info-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 25px rgba(102, 126, 234, 0.5);
}

.info-button.active {
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
}

.dark .info-button.active {
  background: linear-gradient(135deg, #1e40af 0%, #1d4ed8 100%);
}

.info-icon {
  font-size: 1.5rem;
}

.info-text {
  font-size: 0.9rem;
}

.info-popup {
  position: absolute;
  bottom: 100%;
  right: 0;
  width: 400px;
  max-height: 500px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  margin-bottom: 1rem;
  overflow: hidden;
  transition: background 0.3s ease;
}

/* 다크모드에서 팝업 */
.dark .info-popup {
  background: #1e293b;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.4);
}

.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 16px 16px 0 0;
  transition: background 0.3s ease;
}

/* 다크모드에서 팝업 헤더 */
.dark .popup-header {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.popup-header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
}

.close-button {
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.close-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.popup-content {
  padding: 1.5rem;
  overflow-y: auto;
  max-height: 400px;
}

.brand-intro {
  text-align: center;
  margin-bottom: 1.5rem;
}

.brand-logo {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 1rem;
}

.logo-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 0.75rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 로고 아이콘 */
.dark .logo-icon {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.logo-icon svg {
  color: white;
}

.brand-logo h4 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e3c72;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 브랜드 로고 */
.dark .brand-logo h4 {
  color: #f8fafc;
}

.brand-slogan {
  font-size: 0.9rem;
  color: #6b7280;
  font-style: italic;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 브랜드 슬로건 */
.dark .brand-slogan {
  color: #9ca3af;
}

.features-list {
  margin-bottom: 1.5rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid #f1f5f9;
  transition: border-color 0.3s ease;
}

/* 다크모드에서 기능 아이템 */
.dark .feature-item {
  border-bottom: 1px solid #374151;
}

.feature-item:last-child {
  border-bottom: none;
}

.feature-icon {
  font-size: 1.2rem;
  width: 24px;
  text-align: center;
}

.feature-text {
  font-size: 0.9rem;
  color: #374151;
  font-weight: 500;
  transition: color 0.3s ease;
}

/* 다크모드에서 기능 텍스트 */
.dark .feature-text {
  color: #e2e8f0;
}

.description {
  background: #f8fafc;
  padding: 1rem;
  border-radius: 8px;
  border-left: 4px solid #667eea;
  transition: background 0.3s ease, border-color 0.3s ease;
}

/* 다크모드에서 설명 */
.dark .description {
  background: #0f172a;
  border-left-color: #3b82f6;
}

.description p {
  margin: 0;
  font-size: 0.85rem;
  line-height: 1.5;
  color: #6b7280;
  transition: color 0.3s ease;
}

/* 다크모드에서 설명 텍스트 */
.dark .description p {
  color: #cbd5e1;
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .waynai-info-container {
    bottom: 1rem;
    right: 1rem;
  }
  
  .info-popup {
    width: 320px;
    max-height: 400px;
  }
  
  .info-text {
    display: none;
  }
  
  .info-button {
    padding: 1rem;
    border-radius: 50%;
  }
  
  .popup-content {
    padding: 1rem;
  }
  
  .brand-logo h4 {
    font-size: 1.25rem;
  }
  
  .feature-text {
    font-size: 0.85rem;
  }
  
  .description p {
    font-size: 0.8rem;
  }
}
</style> 