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
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || 'http://3.35.206.187:8080'}/api/travel/plan?query=${encodeURIComponent(searchQuery.value)}`);
    
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
/* ========== HomeView — Material Design 3 ========== */
.home {
  min-height: calc(100vh - 64px);
  background: var(--m3-background);
  color: var(--m3-on-background);
  display: flex;
  flex-direction: column;
}

/* Hero: primary-container tonal 배경 */
.hero-section {
  position: relative;
  isolation: isolate;
  padding: clamp(3rem, 8vw, 6rem) 1.5rem clamp(3rem, 6vw, 4.5rem);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--m3-primary-container) 80%, var(--m3-background)) 0%,
    var(--m3-background) 100%
  );
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  overflow: hidden;
}
.hero-section::before {
  content: '';
  position: absolute;
  inset: -50% -20% auto auto;
  width: 60%;
  height: 60%;
  background: radial-gradient(
    circle at 70% 30%,
    color-mix(in srgb, var(--m3-tertiary) 28%, transparent),
    transparent 60%
  );
  filter: blur(20px);
  pointer-events: none;
  z-index: -1;
}
.hero-section::after {
  content: '';
  position: absolute;
  inset: auto auto -30% -10%;
  width: 45%;
  height: 45%;
  background: radial-gradient(
    circle,
    color-mix(in srgb, var(--m3-primary) 22%, transparent),
    transparent 65%
  );
  filter: blur(30px);
  pointer-events: none;
  z-index: -1;
}

.hero-content {
  max-width: 760px;
  width: 100%;
}

.hero-title {
  font: var(--m3-display-medium);
  color: var(--m3-on-surface);
  letter-spacing: -0.02em;
  margin: 0 0 1rem;
}

.hero-subtitle {
  font: var(--m3-title-medium);
  font-size: clamp(1rem, 1.1vw + 0.5rem, 1.25rem);
  color: var(--m3-on-surface-variant);
  margin: 0 0 2.5rem;
}

/* Search block */
.search-container {
  width: 100%;
  max-width: 640px;
  margin: 0 auto;
}

.search-title {
  font: var(--m3-title-medium);
  color: var(--m3-on-surface);
  margin: 0 0 1rem;
}

/* M3 Search bar: pill-shaped elevated surface */
.search-input-group {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  background: var(--m3-surface-container-lowest);
  border: 1px solid var(--m3-outline-variant);
  border-radius: var(--m3-shape-full);
  padding: 0.375rem 0.375rem 0.375rem 1.25rem;
  box-shadow: var(--m3-elev-1);
  transition: box-shadow var(--m3-motion-short), border-color var(--m3-motion-short);
}
.search-input-group:focus-within {
  border-color: var(--m3-primary);
  box-shadow: var(--m3-elev-2);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  padding: 0.875rem 0;
  font: var(--m3-body-large);
  color: var(--m3-on-surface);
}
.search-input::placeholder { color: var(--m3-outline); }
.search-input:focus { box-shadow: none; }
.search-input:disabled { opacity: 0.5; cursor: not-allowed; }

.search-button {
  flex-shrink: 0;
  min-height: 44px;
  padding: 0 1.5rem;
  border-radius: var(--m3-shape-full);
  background: var(--m3-primary);
  color: var(--m3-on-primary);
  font: var(--m3-label-large);
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--m3-motion-short);
}
.search-button::before {
  content: '';
  position: absolute;
  inset: 0;
  background: currentColor;
  opacity: 0;
  transition: opacity var(--m3-motion-short);
  pointer-events: none;
}
.search-button:hover:not(:disabled) { box-shadow: var(--m3-elev-1); }
.search-button:hover:not(:disabled)::before { opacity: var(--m3-state-hover); }
.search-button:active:not(:disabled)::before { opacity: var(--m3-state-pressed); }
.search-button:disabled { opacity: 0.5; cursor: not-allowed; }

.loading-spinner-small {
  width: 16px;
  height: 16px;
  border: 2px solid color-mix(in srgb, currentColor 35%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}

/* Result section */
.result-section {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 2rem 1.5rem 3rem;
}

.result-container {
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-xl);
  padding: clamp(1.5rem, 3vw, 2.5rem);
  box-shadow: var(--m3-elev-1);
  transition: background var(--m3-motion-medium), box-shadow var(--m3-motion-medium);
  min-height: 240px;
}

.initial-state {
  text-align: center;
  padding: 3rem 1.5rem;
  color: var(--m3-on-surface-variant);
}
.initial-icon {
  font-size: 3.5rem;
  margin-bottom: 1rem;
  line-height: 1;
}
.initial-state h3 {
  font: var(--m3-title-large);
  color: var(--m3-on-surface);
  margin: 0 0 0.5rem;
}
.initial-state p {
  font: var(--m3-body-medium);
  color: var(--m3-on-surface-variant);
  margin: 0;
}

.loading-container {
  text-align: center;
  padding: 2.5rem 1rem;
  color: var(--m3-on-surface-variant);
}
.loading-spinner {
  border: 4px solid color-mix(in srgb, var(--m3-primary) 18%, transparent);
  border-top-color: var(--m3-primary);
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 900ms linear infinite;
  margin: 0 auto 0.75rem;
}
.loading-text {
  font: var(--m3-body-large);
  color: var(--m3-on-surface-variant);
}

/* Responsive */
@media (max-width: 768px) {
  .hero-section { padding: 2.5rem 1rem 2rem; }
  .hero-subtitle { margin-bottom: 2rem; }
  .search-input-group {
    flex-direction: row;
    padding: 0.25rem 0.25rem 0.25rem 1rem;
  }
  .search-button { padding: 0 1.125rem; }
  .result-section { padding: 1.25rem 0.75rem 2rem; }
}

@media (max-width: 480px) {
  .hero-title { font-size: 1.875rem; line-height: 1.2; }
  .hero-subtitle { font-size: 0.9375rem; }
  .search-input-group {
    flex-direction: column;
    border-radius: var(--m3-shape-lg);
    padding: 0.75rem;
    gap: 0.75rem;
  }
  .search-input { padding: 0.5rem 0.5rem; width: 100%; }
  .search-button { width: 100%; min-height: 48px; }
  .result-container { padding: 1.25rem; border-radius: var(--m3-shape-lg); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 300ms var(--m3-easing-standard),
              transform 300ms var(--m3-easing-standard);
}
.fade-enter-from { opacity: 0; transform: translateY(12px); }
.fade-leave-to   { opacity: 0; transform: translateY(-12px); }
.fade-enter-to, .fade-leave-from { opacity: 1; transform: translateY(0); }
</style>
