<script setup lang="ts">
import SearchInput from '@/components/SearchInput.vue';
import SearchResult from '@/components/SearchResult.vue';
import { useSearchStore } from '@/stores/search';
import { computed, ref } from 'vue';

const searchStore = useSearchStore();
const searchState = searchStore.state;
const searchQuery = ref('');

const shouldShowResult = computed(() => {
  return (searchState.result !== null && searchState.result !== undefined) || 
         searchState.error || 
         searchState.currentStatus === 'completed';
});

const shouldShowLoading = computed(() => {
  return searchState.isSearching;
});

const handleSearch = () => {
  if (searchQuery.value.trim()) {
    // 검색 로직 구현
    console.log('검색:', searchQuery.value);
  }
};
</script>

<template>
  <div class="home">
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-text">✨ AI 기반 여행 파트너</span>
        </div>
        <h1 class="hero-title">당신의 여행 길을<br>함께 찾아드립니다</h1>
        <p class="hero-subtitle">
          WaynAI는 단순한 정보 검색이 아닌, 당신의 취향과 상황에 맞춘 
          <strong>맞춤형 여행 동반자</strong>입니다
        </p>
        <div class="hero-features">
          <div class="feature-item">
            <div class="feature-icon">🧠</div>
            <span>AI 여행 플래너</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">🗺️</div>
            <span>루트 최적화</span>
          </div>
          <div class="feature-item">
            <div class="feature-icon">💬</div>
            <span>대화형 가이드</span>
          </div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="floating-card card-1">
          <div class="card-icon">📍</div>
          <div class="card-content">
            <h4>경복궁</h4>
            <p>조선왕조의 정궁</p>
          </div>
        </div>
        <div class="floating-card card-2">
          <div class="card-icon">🍜</div>
          <div class="card-content">
            <h4>명동칼국수</h4>
            <p>현지 맛집 추천</p>
          </div>
        </div>
        <div class="floating-card card-3">
          <div class="card-icon">🚇</div>
          <div class="card-content">
            <h4>지하철 3호선</h4>
            <p>최적 경로 안내</p>
          </div>
        </div>
      </div>
    </div>

    <div class="search-section">
      <div class="search-container">
        <div class="search-header">
          <h2 class="search-title">어디로 가고 싶으신가요?</h2>
          <p class="search-description">
            키워드나 여행 계획을 자유롭게 입력해보세요. AI가 당신만의 특별한 여행을 설계해드립니다.
          </p>
        </div>
        <div class="home-search">
          <input
            v-model="searchQuery"
            @keyup.enter="handleSearch"
            type="text"
            placeholder="여행하고 싶은 곳이나 키워드를 입력하세요..."
            class="home-search-input"
          />
          <button @click="handleSearch" class="home-search-button">
            검색
          </button>
        </div>
        
        <!-- 로딩 상태 -->
        <div v-if="shouldShowLoading" class="loading-container">
          <div class="loading-spinner"></div>
          <p class="loading-text">AI가 여행 정보를 생성하고 있습니다...</p>
        </div>
        
        <!-- 결과 표시 -->
        <transition name="fade">
          <SearchResult v-if="shouldShowResult" />
        </transition>
      </div>
    </div>

    <div class="features-section">
      <div class="features-container">
        <div class="section-header">
          <h2 class="section-title">WaynAI만의 특별한 기능</h2>
          <p class="section-subtitle">AI 기술로 구현하는 차별화된 여행 경험</p>
        </div>
        
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">🤖</div>
            </div>
            <h3 class="feature-title">AI 여행 플래너</h3>
            <p class="feature-description">
              날짜, 예산, 관심사를 입력하면 AI가 최적의 여행 일정을 자동으로 생성합니다. 
              개인화된 여행 경험을 설계해드립니다.
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">💬</div>
            </div>
            <h3 class="feature-title">대화형 여행 가이드</h3>
            <p class="feature-description">
              자연어로 자유롭게 질문하세요. 여행지 설명, 역사/문화 맥락, 
              음식 추천까지 GPT 기반으로 답변해드립니다.
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">🎯</div>
            </div>
            <h3 class="feature-title">현지 맞춤 추천</h3>
            <p class="feature-description">
              날씨, 시간대, 계절, 현재 트렌드를 고려한 현지 맞춤 추천. 
              신뢰도 높은 현지 정보를 바탕으로 정확한 추천을 제공합니다.
            </p>
          </div>

          <div class="feature-card">
            <div class="feature-icon-wrapper">
              <div class="feature-icon">🗺️</div>
            </div>
            <h3 class="feature-title">루트 최적화</h3>
            <p class="feature-description">
              지도 기반 추천 코스와 실시간 교통 정보를 제공합니다. 
              효율적인 동선으로 더 많은 곳을 둘러볼 수 있습니다.
            </p>
          </div>
        </div>
      </div>
    </div>

    <div class="cta-section">
      <div class="cta-container">
        <h2 class="cta-title">지금 바로 시작해보세요</h2>
        <p class="cta-description">
          "어디로 갈까?" 고민할 때, WaynAI가 방향을 제시해드립니다
        </p>
        <div class="cta-buttons">
          <router-link to="/travel-plan" class="cta-button primary">여행 계획 시작하기</router-link>
          <router-link to="/about" class="cta-button secondary">더 알아보기</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  transition: background 0.3s ease;
}

/* 다크모드에서 홈 배경 */
.dark .home {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}



.hero-section {
  padding: 2rem;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4rem;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  min-height: 80vh;
}

.hero-content {
  color: white;
}

.hero-badge {
  margin-bottom: 2rem;
}

.badge-text {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 1.5rem;
  background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.25rem;
  line-height: 1.6;
  margin-bottom: 2rem;
  opacity: 0.9;
  color: #e0e7ff;
}

.hero-features {
  display: flex;
  gap: 1.5rem;
  flex-wrap: wrap;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  padding: 0.75rem 1rem;
  border-radius: 12px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.feature-icon {
  font-size: 1.25rem;
}

.hero-visual {
  position: relative;
  height: 400px;
}

.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  gap: 1rem;
  animation: float 6s ease-in-out infinite;
  transition: background 0.3s ease;
}

/* 다크모드에서 플로팅 카드 */
.dark .floating-card {
  background: rgba(30, 41, 59, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.card-1 {
  top: 0;
  left: 0;
  animation-delay: 0s;
}

.card-2 {
  top: 50%;
  right: 0;
  animation-delay: 2s;
}

.card-3 {
  bottom: 0;
  left: 20%;
  animation-delay: 4s;
}

.card-icon {
  font-size: 2rem;
}

.card-content h4 {
  margin: 0 0 0.25rem 0;
  font-weight: 600;
  color: #1e3c72;
  transition: color 0.3s ease;
}

.card-content p {
  margin: 0;
  font-size: 0.875rem;
  color: #6b7280;
  transition: color 0.3s ease;
}

/* 다크모드에서 카드 콘텐츠 */
.dark .card-content h4 {
  color: #f8fafc;
}

.dark .card-content p {
  color: #cbd5e1;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

.search-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  padding: 4rem 2rem;
  margin: 2rem auto;
  border-radius: 24px;
  max-width: 1200px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  transition: background 0.3s ease;
}

/* 다크모드에서 검색 섹션 */
.dark .search-section {
  background: rgba(30, 41, 59, 0.95);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.search-container {
  max-width: 800px;
  margin: 0 auto;
}

.search-header {
  text-align: center;
  margin-bottom: 3rem;
}

.search-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e3c72;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

.search-description {
  font-size: 1.125rem;
  color: #6b7280;
  line-height: 1.6;
  transition: color 0.3s ease;
}

/* 다크모드에서 검색 제목과 설명 */
.dark .search-title {
  color: #f8fafc;
}

.dark .search-description {
  color: #cbd5e1;
}

.loading-container {
  text-align: center;
  padding: 2rem 0;
  color: #6b7280;
}

.loading-spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-top: 4px solid #1e3c72;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 1.125rem;
}

.features-section {
  padding: 6rem 2rem;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
  transition: background 0.3s ease;
}

/* 다크모드에서 피처 섹션 */
.dark .features-section {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.features-container {
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 4rem;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e3c72;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

.section-subtitle {
  font-size: 1.125rem;
  color: #6b7280;
  transition: color 0.3s ease;
}

/* 다크모드에서 섹션 제목과 부제목 */
.dark .section-title {
  color: #f8fafc;
}

.dark .section-subtitle {
  color: #cbd5e1;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.feature-card {
  background: white;
  padding: 2.5rem;
  border-radius: 20px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  text-align: center;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

/* 다크모드에서 피처 카드 */
.dark .feature-card {
  background: #1e293b;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.dark .feature-card:hover {
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.feature-icon-wrapper {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 1.5rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 피처 아이콘 래퍼 */
.dark .feature-icon-wrapper {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.feature-icon {
  font-size: 2rem;
}

.feature-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1e3c72;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

.feature-description {
  color: #6b7280;
  line-height: 1.6;
  transition: color 0.3s ease;
}

/* 다크모드에서 피처 텍스트 */
.dark .feature-title {
  color: #f8fafc;
}

.dark .feature-description {
  color: #cbd5e1;
}

.cta-section {
  padding: 6rem 2rem;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  text-align: center;
  color: white;
  transition: background 0.3s ease;
}

/* 다크모드에서 CTA 섹션 */
.dark .cta-section {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.cta-container {
  max-width: 600px;
  margin: 0 auto;
}

.cta-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
}

.cta-description {
  font-size: 1.125rem;
  opacity: 0.9;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.cta-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.cta-button {
  padding: 1rem 2rem;
  border-radius: 12px;
  font-weight: 600;
  font-size: 1rem;
  border: none;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-block;
}

.cta-button.primary {
  background: white;
  color: #1e3c72;
}

.cta-button.primary:hover {
  background: #f8fafc;
  transform: translateY(-2px);
}

.cta-button.secondary {
  background: transparent;
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.cta-button.secondary:hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.5);
}

.home-search {
  display: flex;
  gap: 1rem;
  max-width: 600px;
  margin: 0 auto;
}

.home-search-input {
  flex: 1;
  padding: 1rem 1.5rem;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.3s ease;
  background: white;
  color: #374151;
}

.home-search-input:focus {
  border-color: #667eea;
}

/* 다크모드에서 홈 검색 입력 필드 */
.dark .home-search-input {
  background: #1e293b;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .home-search-input:focus {
  border-color: #60a5fa;
}

.home-search-button {
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease;
}

/* 다크모드에서 홈 검색 버튼 */
.dark .home-search-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.home-search-button:hover {
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  
  .hero-section {
    grid-template-columns: 1fr;
    gap: 2rem;
    padding: 4rem 1rem 2rem;
    text-align: center;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1.125rem;
  }

  .hero-features {
    justify-content: center;
    flex-direction: column;
    gap: 1rem;
  }

  .hero-visual {
    height: 300px;
  }

  .floating-card {
    padding: 1rem;
  }

  .search-section {
    margin: 1rem;
    padding: 2rem 1rem;
  }

  .search-title {
    font-size: 2rem;
  }

  .features-section {
    padding: 3rem 1rem;
  }

  .section-title {
    font-size: 2rem;
  }

  .feature-card {
    padding: 2rem;
  }

  .cta-section {
    padding: 3rem 1rem;
  }

  .cta-title {
    font-size: 2rem;
  }

  .cta-buttons {
    flex-direction: column;
    align-items: center;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-subtitle {
    font-size: 1rem;
  }
  
  .search-container {
    padding: 1rem;
  }
  
  .search-title {
    font-size: 1.8rem;
  }
  
  .floating-card {
    padding: 0.75rem;
  }
  
  .card-content h4 {
    font-size: 0.9rem;
  }
  
  .card-content p {
    font-size: 0.7rem;
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
