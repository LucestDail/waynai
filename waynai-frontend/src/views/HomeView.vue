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
        <h1 class="hero-title">당신의 여행 길을<br>함께 찾아드립니다</h1>
        <p class="hero-subtitle">
          AI가 이끄는 당신만의 특별한 여행 경험
        </p>
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
  color: #374151;
  line-height: 1.6;
  transition: color 0.3s ease;
}

/* 다크모드에서 검색 제목과 설명 */
.dark .search-title {
  color: #f8fafc;
}

.dark .search-description {
  color: #e2e8f0;
}

.loading-container {
  text-align: center;
  padding: 2rem 0;
  color: #6b7280;
  transition: color 0.3s ease;
}

.dark .loading-container {
  color: #9ca3af;
}

.loading-spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-top: 4px solid #1e3c72;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
  transition: border-color 0.3s ease;
}

.dark .loading-spinner {
  border: 4px solid rgba(255, 255, 255, 0.1);
  border-top: 4px solid #60a5fa;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  font-size: 1.125rem;
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
    padding: 2rem 1rem;
    text-align: center;
    min-height: 60vh;
  }

  .hero-title {
    font-size: 2.5rem;
    margin-bottom: 1rem;
  }

  .hero-subtitle {
    font-size: 1.125rem;
    margin-bottom: 2rem;
  }

  .hero-visual {
    height: 250px;
    margin-top: 2rem;
  }

  .floating-card {
    padding: 0.75rem;
    font-size: 0.875rem;
  }

  .card-content h4 {
    font-size: 0.9rem;
    margin-bottom: 0.25rem;
  }

  .card-content p {
    font-size: 0.75rem;
  }

  .search-section {
    margin: 1rem;
    padding: 2rem 1rem;
    border-radius: 16px;
  }

  .search-title {
    font-size: 1.75rem;
    margin-bottom: 0.75rem;
  }

  .search-description {
    font-size: 1rem;
    margin-bottom: 2rem;
  }

  .home-search {
    flex-direction: column;
    gap: 0.75rem;
  }

  .home-search-input {
    padding: 0.875rem 1rem;
    font-size: 0.9rem;
  }

  .home-search-button {
    padding: 0.875rem 1.5rem;
    font-size: 0.9rem;
  }
}

@media (max-width: 480px) {
  .hero-section {
    padding: 1.5rem 0.75rem;
    min-height: 50vh;
  }

  .hero-title {
    font-size: 1.75rem;
    line-height: 1.3;
  }
  
  .hero-subtitle {
    font-size: 0.9rem;
    margin-bottom: 1.5rem;
  }

  .hero-visual {
    height: 200px;
    margin-top: 1.5rem;
  }
  
  .search-section {
    margin: 0.75rem;
    padding: 1.5rem 0.75rem;
    border-radius: 12px;
  }
  
  .search-title {
    font-size: 1.5rem;
    margin-bottom: 0.5rem;
  }

  .search-description {
    font-size: 0.9rem;
    margin-bottom: 1.5rem;
  }
  
  .floating-card {
    padding: 0.5rem;
    font-size: 0.8rem;
  }
  
  .card-content h4 {
    font-size: 0.8rem;
    margin-bottom: 0.2rem;
  }
  
  .card-content p {
    font-size: 0.7rem;
  }

  .home-search-input {
    padding: 0.75rem 0.875rem;
    font-size: 0.85rem;
  }

  .home-search-button {
    padding: 0.75rem 1.25rem;
    font-size: 0.85rem;
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
