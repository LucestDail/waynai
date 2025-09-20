<script setup lang="ts">
import SearchInput from '@/components/SearchInput.vue';
import SearchResult from '@/components/SearchResult.vue';
import SearchProgress from '@/components/SearchProgress.vue';
import StreamResult from '@/components/StreamResult.vue';
import { useSearchStore } from '@/stores/search';
import { useStreamStore } from '@/stores/stream';
import { computed, ref } from 'vue';

const searchStore = useSearchStore();
const streamStore = useStreamStore();
const searchState = searchStore.state;
const streamState = streamStore.state;
const searchQuery = ref('');
const isSearching = ref(false);

const shouldShowResult = computed(() => {
  return (searchState.result !== null && searchState.result !== undefined) || 
         searchState.error || 
         searchState.currentStatus === 'completed';
});

const shouldShowLoading = computed(() => {
  return isSearching.value;
});

const shouldShowStream = computed(() => {
  // 데이터가 있거나 에러가 있으면 결과 컴포넌트 표시
  const hasData = streamState.currentData && streamState.currentData.trim().length > 0;
  const hasError = !!streamState.error;
  
  console.log('결과 표시 여부:', {
    hasData,
    hasError,
    currentDataLength: streamState.currentData?.length || 0,
    willShow: hasData || hasError
  });
  
  return hasData || hasError;
});

const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    return;
  }
  
  // 중복 호출 방지
  if (isSearching.value) {
    console.log('이미 검색 중입니다. 중복 호출을 차단합니다.');
    return;
  }
  
  console.log('여행 계획 생성 시작:', searchQuery.value);
  isSearching.value = true;
  
  // 검색 시작 시 스크롤 다운 처리
  setTimeout(() => {
    const resultSection = document.querySelector('.result-section');
    if (resultSection) {
      resultSection.scrollIntoView({ behavior: 'smooth' });
    }
  }, 100);
  
  // searchStore의 isSearching 상태도 함께 설정
  searchStore.state.isSearching = true;
  
  // 기존 검색 결과 클리어
  searchStore.clearSearch();
  streamStore.clearStream();
  
  try {
    // 30초 프로그레스바 시뮬레이션 시작
    let progressValue = 0;
    let currentStepIndex = 0;
    
    const steps = [
      { status: 'analyzing', step: 'intent_analysis', message: '사용자 입력을 분석하는 중...' },
      { status: 'searching', step: 'knowledge_search', message: '관광지 정보를 검색하고 수집하는 중...' },
      { status: 'generating', step: 'course_generation', message: 'AI가 여행 계획을 생성하는 중...' }
    ];
    
    // 프로그레스바 시뮬레이션 함수
    const simulateProgress = () => {
      const progressInterval = setInterval(() => {
        progressValue += 1;
        
        // 단계별 진행 (10초마다 단계 변경)
        if (progressValue % 10 === 0 && currentStepIndex < steps.length - 1) {
          currentStepIndex++;
          const currentStep = steps[currentStepIndex];
          searchStore.state.currentStatus = currentStep.status;
          searchStore.state.currentStep = currentStep.step;
          searchStore.state.progress = steps.slice(0, currentStepIndex + 1).map(s => s.message);
        }
        
        // 30초 후 자동으로 100%로 설정
        if (progressValue >= 30) {
          clearInterval(progressInterval);
        }
      }, 1000);
      
      return progressInterval;
    };
    
    // 프로그레스바 시뮬레이션 시작
    const progressInterval = simulateProgress();
    
    // HTTP 요청과 프로그레스바 시뮬레이션을 병렬로 실행
    console.log('여행 계획 요청 시작:', searchQuery.value);
    const response = await fetch(`http://localhost:8080/api/travel/plan?query=${encodeURIComponent(searchQuery.value)}`);
    
    // 프로그레스바 시뮬레이션 중단
    clearInterval(progressInterval);
    
    if (!response.ok) {
      throw new Error(`HTTP 오류! 상태: ${response.status}`);
    }
    
    const data = await response.text();
    console.log('서버 응답 받음, 데이터 길이:', data.length);
    
    // 완료 상태로 즉시 설정
    searchStore.state.currentStatus = 'completed';
    searchStore.state.currentStep = 'course_generation';
    searchStore.state.progress = [
      '사용자 입력을 분석하는 중...',
      '관광지 정보를 검색하고 수집하는 중...',
      'AI가 여행 계획을 생성하는 중...',
      '여행 계획 생성 완료!'
    ];
    
    // 1초 지연 후 프로그레스바 페이드아웃
    setTimeout(() => {
      // 프로그레스바 페이드아웃 효과
      const progressElement = document.querySelector('.progress-container') as HTMLElement;
      if (progressElement) {
        progressElement.style.transition = 'opacity 0.8s ease-out';
        progressElement.style.opacity = '0';
        
        // 페이드아웃 완료 후 데이터 표시
        setTimeout(() => {
          if (data && data.trim().length > 0) {
            streamStore.setData(data);
            console.log('데이터 설정 완료, 화면에 표시됨');
          } else {
            streamStore.setError('서버에서 빈 응답을 받았습니다.');
          }
        }, 800); // 페이드아웃 완료 후 0.8초 대기
      } else {
        // 프로그레스바가 없는 경우 바로 데이터 표시
        if (data && data.trim().length > 0) {
          streamStore.setData(data);
          console.log('데이터 설정 완료, 화면에 표시됨');
        } else {
          streamStore.setError('서버에서 빈 응답을 받았습니다.');
        }
      }
    }, 1000);
    
  } catch (error) {
    console.error('여행 계획 생성 실패:', error);
    
    // 에러 상태 업데이트
    searchStore.state.currentStatus = 'error';
    searchStore.state.progress = [
      '사용자 입력을 분석하는 중...',
      'AI가 여행 계획을 생성하는 중...',
      '오류가 발생했습니다.'
    ];
    
    streamStore.setError(error instanceof Error ? error.message : '알 수 없는 오류가 발생했습니다.');
  } finally {
    isSearching.value = false;
    searchStore.state.isSearching = false;
  }
};
</script>

<template>
  <div class="home">
    <!-- 메인 히어로 섹션 -->
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">당신의 여행 길을<br>함께 찾아드립니다</h1>
        <p class="hero-subtitle">
          AI가 이끄는 당신만의 특별한 여행 경험
        </p>
        
        <!-- 검색 섹션을 히어로 섹션 안으로 이동 -->
        <div class="search-container">
          <h2 class="search-title">어디로 가고 싶으신가요?</h2>
          <div class="search-input-group">
            <input
              v-model="searchQuery"
              @keyup.enter="handleSearch"
              type="text"
              placeholder="서울 종로구"
              class="search-input"
              :disabled="isSearching"
            />
            <button @click="handleSearch" class="search-button" :disabled="isSearching">
              <span v-if="isSearching" class="loading-spinner-small"></span>
              <span v-else>검색</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 결과 표시 섹션 -->
    <div class="result-section">
      <div class="result-container">
        <transition name="fade" mode="out-in">
          <SearchProgress v-if="searchState.isSearching" key="progress" />
          
          <!-- 스트림 결과 표시 -->
          <StreamResult v-else-if="shouldShowStream" key="result" />
          
          <!-- 로딩 상태 (백업용) -->
          <div v-else-if="shouldShowLoading" key="loading" class="loading-container">
            <div class="loading-spinner"></div>
            <p class="loading-text">AI가 여행 정보를 생성하고 있습니다...</p>
          </div>
          
          <!-- 초기 상태 -->
          <div v-else key="initial" class="initial-state">
            <div class="initial-icon">✈️</div>
            <h3>여행 계획을 시작해보세요</h3>
            <p>위에서 여행하고 싶은 곳을 입력하고 검색 버튼을 눌러보세요.</p>
          </div>
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
  padding: 4rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
  overflow: hidden;
}

.hero-content {
  color: white;
  max-width: 800px;
  width: 100%;
}

.hero-title {
  font-size: 4rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 1.5rem;
  background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 1.5rem;
  line-height: 1.6;
  margin-bottom: 4rem;
  opacity: 0.9;
  color: #e0e7ff;
}

.search-container {
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

.search-title {
  font-size: 2rem;
  font-weight: 600;
  color: white;
  margin-bottom: 2rem;
  opacity: 0.95;
}

.search-input-group {
  display: flex;
  gap: 1rem;
  width: 100%;
}

.search-input {
  flex: 1;
  padding: 1.25rem 1.5rem;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  font-size: 1.1rem;
  outline: none;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  backdrop-filter: blur(10px);
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.7);
}

.search-input:focus {
  border-color: rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.15);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.1);
}

.search-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.search-button {
  padding: 1.25rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 1.1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.search-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}

.search-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.loading-spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 결과 섹션 */
.result-section {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  margin: 0 auto;
  max-width: 1200px;
  transition: background 0.3s ease;
}

/* 다크모드에서 결과 섹션 */
.dark .result-section {
  background: rgba(30, 41, 59, 0.95);
}

.result-container {
  padding: 2rem;
}

.initial-state {
  text-align: center;
  padding: 4rem 2rem;
  color: #6b7280;
  transition: color 0.3s ease;
}

.dark .initial-state {
  color: #9ca3af;
}

.initial-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  opacity: 0.7;
}

.initial-state h3 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #374151;
  transition: color 0.3s ease;
}

.dark .initial-state h3 {
  color: #e2e8f0;
}

.initial-state p {
  font-size: 1rem;
  line-height: 1.6;
  margin: 0;
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

@media (max-width: 768px) {
  .hero-section {
    padding: 2rem 1rem;
    height: 100vh;
  }

  .hero-title {
    font-size: 2.5rem;
    margin-bottom: 1rem;
  }

  .hero-subtitle {
    font-size: 1.125rem;
    margin-bottom: 3rem;
  }

  .search-title {
    font-size: 1.5rem;
    margin-bottom: 1.5rem;
  }

  .search-input-group {
    flex-direction: column;
    gap: 1rem;
  }

  .search-input {
    padding: 1rem 1.25rem;
    font-size: 1rem;
  }

  .search-button {
    padding: 1rem 1.5rem;
    font-size: 1rem;
    min-width: auto;
  }

  .result-container {
    padding: 1rem;
  }
}

@media (max-width: 480px) {
  .hero-section {
    padding: 1.5rem 0.75rem;
    height: 100vh;
  }

  .hero-title {
    font-size: 2rem;
    line-height: 1.3;
  }
  
  .hero-subtitle {
    font-size: 1rem;
    margin-bottom: 2.5rem;
  }

  .search-title {
    font-size: 1.25rem;
    margin-bottom: 1.25rem;
  }

  .search-input {
    padding: 0.875rem 1rem;
    font-size: 0.9rem;
  }

  .search-button {
    padding: 0.875rem 1.25rem;
    font-size: 0.9rem;
  }

  .result-container {
    padding: 0.75rem;
  }
}

.fade-enter-active, .fade-leave-active {
  transition: all 0.8s ease-in-out;
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(20px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}
.fade-enter-to, .fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}
</style>
