<template>
  <div class="result-container">
    <div class="result-header">
      <h3 class="result-title">검색 결과</h3>
      <div v-if="state.currentStatus === 'completed'" class="completion-badge">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z" fill="currentColor"/>
        </svg>
        완료
      </div>
    </div>

    <div v-if="state.error" class="error-result">
      <div class="error-content">
        <div class="error-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
          </svg>
        </div>
        <h4 class="error-title">검색 중 오류가 발생했습니다</h4>
        <p class="error-description">{{ state.error }}</p>
        <button @click="handleRetry" class="retry-button">
          다시 시도
        </button>
      </div>
    </div>

    <!-- 완료되었지만 결과가 없는 경우 -->
    <div v-else-if="state.currentStatus === 'completed' && !state.result && !state.error" class="error-result">
      <div class="error-content">
        <div class="error-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
          </svg>
        </div>
        <h4 class="error-title">결과를 생성할 수 없습니다</h4>
        <p class="error-description">검색이 완료되었지만 결과를 생성할 수 없었습니다. 다시 시도해주세요.</p>
        <button @click="handleRetry" class="retry-button">
          다시 시도
        </button>
      </div>
    </div>

    <div v-else-if="state.result && isErrorResult" class="error-result">
      <div class="error-content">
        <div class="error-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
          </svg>
        </div>
        <h4 class="error-title">AI 서비스 일시적 오류</h4>
        <p class="error-description">{{ state.result }}</p>
        <button @click="handleRetry" class="retry-button">
          다시 시도
        </button>
      </div>
    </div>

    <div v-else-if="state.result" class="success-result">
      <div class="result-content">
        <div class="result-type-indicator">
          <span class="type-badge" :class="resultTypeClass">
            {{ getResultTypeText() }}
          </span>
        </div>

        <div class="result-body">
          <!-- 추천 결과 표시 -->
          <div v-if="isRecommendationResult" class="recommendation-result">
            <div class="result-summary">
              <h4 class="summary-title">{{ parsedResult.keyword }} 추천 관광지</h4>
              <p class="summary-description">{{ parsedResult.summary }}</p>
            </div>
            
            <div class="spots-grid">
              <div v-for="(spot, index) in parsedResult.spots" :key="index" class="spot-card">
                <div class="spot-header">
                  <h5 class="spot-name">{{ spot.name }}</h5>
                  <div class="spot-rating" v-if="spot.rating">
                    <span class="rating-text">{{ spot.rating }}</span>
                    <div class="rating-stars">
                      <span v-for="i in 5" :key="i" class="star" :class="{ filled: i <= Math.round(spot.rating) }">★</span>
                    </div>
                  </div>
                </div>
                <p class="spot-description">{{ spot.description }}</p>
                <div class="spot-details">
                  <span class="spot-category">{{ spot.category }}</span>
                  <span class="spot-address">{{ spot.address }}</span>
                </div>
                <div v-if="spot.highlights && spot.highlights.length > 0" class="spot-highlights">
                  <h6 class="highlights-title">하이라이트</h6>
                  <div class="highlights-list">
                    <span v-for="(highlight, hIndex) in spot.highlights" :key="hIndex" class="highlight-tag">
                      {{ highlight }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 여행 계획 결과 표시 -->
          <div v-else-if="isTravelPlanResult" class="travel-plan-result">
            <div class="result-summary">
              <h4 class="summary-title">{{ parsedResult.destination }} 여행 계획</h4>
              <p class="summary-description">{{ parsedResult.duration }} - {{ parsedResult.summary }}</p>
              <div class="plan-meta">
                <span class="meta-item">
                  <strong>예상 비용:</strong> {{ parsedResult.budget }}
                </span>
                <span class="meta-item">
                  <strong>교통:</strong> {{ parsedResult.transportation }}
                </span>
                <span class="meta-item">
                  <strong>숙박:</strong> {{ parsedResult.accommodation }}
                </span>
              </div>
            </div>
            
            <div class="itinerary-section">
              <h5 class="section-title">여행 일정</h5>
              <div class="itinerary-list">
                <div v-for="day in parsedResult.itinerary" :key="day.day" class="day-plan">
                  <h6 class="day-title">{{ day.title }}</h6>
                  <p class="day-overview">{{ day.overview }}</p>
                  <div class="day-content">
                    <div class="activities-section">
                      <h7 class="subsection-title">활동</h7>
                      <ul class="activities-list">
                        <li v-for="(activity, index) in day.activities" :key="index" class="activity-item">
                          {{ activity }}
                        </li>
                      </ul>
                    </div>
                    <div v-if="day.spots && day.spots.length > 0" class="spots-section">
                      <h7 class="subsection-title">방문지</h7>
                      <ul class="spots-list">
                        <li v-for="(spot, index) in day.spots" :key="index" class="spot-item">
                          {{ spot }}
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="parsedResult.tips && parsedResult.tips.length > 0" class="tips-section">
              <h5 class="section-title">여행 팁</h5>
              <ul class="tips-list">
                <li v-for="(tip, index) in parsedResult.tips" :key="index" class="tip-item">
                  {{ tip }}
                </li>
              </ul>
            </div>
          </div>

          <!-- 일반 JSON 결과 표시 -->
          <div v-else-if="isJsonResult" class="json-result">
            <div class="structured-result">
              <div v-if="parsedJsonResult && Array.isArray(parsedJsonResult)" v-for="(item, index) in parsedJsonResult" :key="index" class="result-item">
                <h4 class="item-title">{{ item.name || item.title || `항목 ${index + 1}` }}</h4>
                <p class="item-description">{{ item.description || item.content || item.summary || '설명 없음' }}</p>
              </div>
              <div v-else-if="parsedResult" class="result-item">
                <h4 class="item-title">검색 결과</h4>
                <p class="item-description">{{ parsedResult.summary || parsedResult.description || '결과가 성공적으로 생성되었습니다.' }}</p>
              </div>
            </div>
            <details class="json-details">
              <summary>JSON 데이터 보기</summary>
              <pre class="json-content">{{ formatJson(state.result) }}</pre>
            </details>
          </div>

          <!-- 텍스트 결과 표시 -->
          <div v-else class="text-result">
            <div class="result-text" v-html="formatTextResult(state.result)"></div>
            <details class="json-details">
              <summary>원본 데이터 보기</summary>
              <pre class="json-content">{{ state.result }}</pre>
            </details>
          </div>
        </div>

        <div class="result-actions">
          <button @click="handleCopy" class="action-button copy-button">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z" fill="currentColor"/>
            </svg>
            복사
          </button>
          <button @click="handleDownload" class="action-button download-button">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z" fill="currentColor"/>
            </svg>
            다운로드
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSearchStore } from '@/stores/search';

const searchStore = useSearchStore();
const { state } = searchStore;

const isErrorResult = computed(() => {
  if (state.error) return true;
  
  if (typeof state.result === 'string') {
    return state.result.includes('오류') || 
           state.result.includes('error') || 
           state.result.includes('Error') ||
           state.result.includes('503') ||
           state.result.includes('Service Unavailable');
  }
  return false;
});

const parsedResult = computed(() => {
  if (typeof state.result === 'string') {
    try {
      console.log('Raw result:', state.result);
      
      // JSON 코드 블록 제거 (```json ... ```)
      let cleanResult = state.result;
      if (cleanResult.startsWith('```json')) {
        cleanResult = cleanResult.replace(/^```json\n/, '').replace(/\n```$/, '');
      } else if (cleanResult.startsWith('```')) {
        cleanResult = cleanResult.replace(/^```\n/, '').replace(/\n```$/, '');
      }
      
      console.log('Cleaned result:', cleanResult);
      
      const parsed = JSON.parse(cleanResult);
      console.log('Parsed result:', parsed);
      return parsed;
    } catch (error) {
      console.error('Failed to parse JSON:', error);
      console.log('Failed to parse result:', state.result);
      return null;
    }
  }
  console.log('Non-string result:', state.result);
  return state.result;
});

const isJsonResult = computed(() => {
  return parsedResult.value !== null;
});

const isRecommendationResult = computed(() => {
  const result = parsedResult.value;
  return result && (result.type === 'recommendation' || (result.spots && Array.isArray(result.spots)));
});

const isTravelPlanResult = computed(() => {
  const result = parsedResult.value;
  return result && (result.type === 'travel_plan' || (result.itinerary && Array.isArray(result.itinerary)));
});

const parsedJsonResult = computed(() => {
  if (typeof state.result === 'string') {
    try {
      // JSON 코드 블록 제거 (```json ... ```)
      let cleanResult = state.result;
      if (cleanResult.startsWith('```json')) {
        cleanResult = cleanResult.replace(/^```json\n/, '').replace(/\n```$/, '');
      } else if (cleanResult.startsWith('```')) {
        cleanResult = cleanResult.replace(/^```\n/, '').replace(/\n```$/, '');
      }
      
      const parsed = JSON.parse(cleanResult);
      console.log('Parsed JSON result:', parsed);
      
      // 배열 형태의 결과인 경우
      if (Array.isArray(parsed)) {
        return parsed;
      }
      // 객체 형태의 결과인 경우
      if (parsed.spots && Array.isArray(parsed.spots)) {
        return parsed.spots;
      }
      return null;
    } catch (error) {
      console.error('Failed to parse JSON result:', error);
      return null;
    }
  }
  return state.result;
});

const resultTypeClass = computed(() => {
  if (isRecommendationResult.value) {
    return 'type-recommendation';
  } else if (isTravelPlanResult.value) {
    return 'type-plan';
  }
  return 'type-general';
});

const getResultTypeText = () => {
  if (isRecommendationResult.value) {
    return '관광지 추천';
  } else if (isTravelPlanResult.value) {
    return '여행 계획';
  }
  return '검색 결과';
};

const formatJson = (data: any) => {
  try {
    if (typeof data === 'string') {
      // JSON 코드 블록 제거 (```json ... ```)
      let cleanResult = data;
      if (cleanResult.startsWith('```json')) {
        cleanResult = cleanResult.replace(/^```json\n/, '').replace(/\n```$/, '');
      } else if (cleanResult.startsWith('```')) {
        cleanResult = cleanResult.replace(/^```\n/, '').replace(/\n```$/, '');
      }
      const parsed = JSON.parse(cleanResult);
      return JSON.stringify(parsed, null, 2);
    }
    return JSON.stringify(data, null, 2);
  } catch (error) {
    console.error('Failed to format JSON:', error);
    return data;
  }
};

const formatTextResult = (text: any) => {
  if (typeof text !== 'string') return text;
  
  // 마크다운 스타일 텍스트를 HTML로 변환
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>') // **텍스트** -> <strong>텍스트</strong>
    .replace(/\n/g, '<br>') // 줄바꿈을 <br>로 변환
    .replace(/•/g, '• ') // 불릿 포인트 정리
    .replace(/📅/g, '<span style="color: #3498db;">📅</span>') // 이모지 색상 추가
    .replace(/💰/g, '<span style="color: #27ae60;">💰</span>')
    .replace(/🎯/g, '<span style="color: #e74c3c;">🎯</span>')
    .replace(/🏛️/g, '<span style="color: #8e44ad;">🏛️</span>')
    .replace(/📍/g, '<span style="color: #e67e22;">📍</span>')
    .replace(/🍜/g, '<span style="color: #f39c12;">🍜</span>')
    .replace(/🚇/g, '<span style="color: #16a085;">🚇</span>')
    .replace(/💡/g, '<span style="color: #f1c40f;">💡</span>');
};

const handleCopy = async () => {
  try {
    const textToCopy = typeof state.result === 'string' 
      ? state.result 
      : JSON.stringify(state.result, null, 2);
    
    // 모바일 환경에서 더 안정적인 복사 방법 사용
    if (navigator.clipboard && window.isSecureContext) {
      // 최신 브라우저에서 Clipboard API 사용
      await navigator.clipboard.writeText(textToCopy);
    } else {
      // 구형 브라우저나 모바일에서 fallback 방법 사용
      await fallbackCopyTextToClipboard(textToCopy);
    }
    
    alert('결과가 클립보드에 복사되었습니다.');
  } catch (error) {
    console.error('Failed to copy:', error);
    alert('복사에 실패했습니다.');
  }
};

const handleDownload = () => {
  try {
    const content = typeof state.result === 'string' 
      ? state.result 
      : JSON.stringify(state.result, null, 2);
    
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `waynai-result-${new Date().toISOString().slice(0, 10)}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Failed to download:', error);
    alert('다운로드에 실패했습니다.');
  }
};

// 모바일 환경을 위한 fallback 복사 함수
const fallbackCopyTextToClipboard = (text: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    // 임시 textarea 요소 생성
    const textArea = document.createElement('textarea');
    textArea.value = text;
    
    // 화면에서 보이지 않도록 설정
    textArea.style.position = 'fixed';
    textArea.style.top = '0';
    textArea.style.left = '0';
    textArea.style.width = '2em';
    textArea.style.height = '2em';
    textArea.style.padding = '0';
    textArea.style.border = 'none';
    textArea.style.outline = 'none';
    textArea.style.boxShadow = 'none';
    textArea.style.background = 'transparent';
    textArea.style.opacity = '0';
    textArea.style.zIndex = '-1';
    
    // DOM에 추가
    document.body.appendChild(textArea);
    
    try {
      // iOS Safari에서 작동하도록 수정
      textArea.focus();
      textArea.select();
      
      // iOS에서 select()가 작동하지 않을 경우를 대비
      if (textArea.setSelectionRange) {
        textArea.setSelectionRange(0, 99999);
      }
      
      // 복사 실행
      const successful = document.execCommand('copy');
      
      if (successful) {
        resolve();
      } else {
        reject(new Error('복사 명령이 실패했습니다.'));
      }
    } catch (err) {
      reject(err);
    } finally {
      // 임시 요소 제거
      document.body.removeChild(textArea);
    }
  });
};

const handleRetry = () => {
  // 재시도 로직 - 현재는 단순히 에러를 클리어
  searchStore.clearSearch();
};
</script>

<style scoped>
.result-container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f1f3f4;
}

.result-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.completion-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background-color: #27ae60;
  color: white;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
}

.error-result {
  text-align: center;
  padding: 3rem 1rem;
}

.error-content {
  max-width: 400px;
  margin: 0 auto;
}

.error-icon {
  margin-bottom: 1rem;
  color: #e74c3c;
}

.error-title {
  font-size: 1.3rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 1rem;
}

.error-description {
  color: #7f8c8d;
  margin-bottom: 2rem;
  line-height: 1.6;
}

.retry-button {
  padding: 0.75rem 2rem;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-button:hover {
  background-color: #2980b9;
  transform: translateY(-2px);
}

.success-result {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.result-type-indicator {
  display: flex;
  justify-content: flex-start;
}

.type-badge {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
}

.type-recommendation {
  background-color: #9b59b6;
}

.type-plan {
  background-color: #e67e22;
}

.type-general {
  background-color: #3498db;
}

.result-body {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  border: 1px solid #e1e8ed;
}

/* 추천 결과 스타일 */
.recommendation-result {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.result-summary {
  text-align: center;
  padding: 1rem;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
}

.summary-title {
  font-size: 1.3rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
}

.summary-description {
  color: #7f8c8d;
  margin: 0;
}

.spots-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1rem;
}

.spot-card {
  padding: 1.5rem;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.spot-card:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.spot-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.spot-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  flex: 1;
}

.spot-rating {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.rating-text {
  font-weight: 600;
  color: #f39c12;
}

.rating-stars {
  display: flex;
  gap: 1px;
}

.star {
  color: #ddd;
  font-size: 0.9rem;
}

.star.filled {
  color: #f39c12;
}

.spot-description {
  font-size: 1rem;
  line-height: 1.6;
  color: #34495e;
  margin: 0 0 1rem 0;
}

.spot-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.spot-category {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  background-color: #3498db;
  color: white;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
  align-self: flex-start;
}

.spot-address {
  font-size: 0.9rem;
  color: #7f8c8d;
}

.spot-highlights {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e1e8ed;
}

.highlights-title {
  font-size: 1rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
}

.highlights-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.highlight-tag {
  display: inline-block;
  padding: 0.3rem 0.7rem;
  background-color: #f1f3f4;
  color: #34495e;
  border-radius: 15px;
  font-size: 0.8rem;
  font-weight: 600;
}

/* 여행 계획 결과 스타일 */
.travel-plan-result {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.plan-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #e1e8ed;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #7f8c8d;
}

.meta-item strong {
  color: #2c3e50;
  font-weight: 600;
}

.itinerary-section,
.tips-section {
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
  padding: 1.5rem;
}

.section-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 1rem 0;
  border-bottom: 2px solid #3498db;
  padding-bottom: 0.5rem;
}

.itinerary-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.day-plan {
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #3498db;
}

.day-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
}

.day-overview {
  font-size: 0.9rem;
  color: #7f8c8d;
  margin-bottom: 0.5rem;
}

.day-content {
  display: flex;
  gap: 1.5rem;
}

.activities-section,
.spots-section {
  flex: 1;
}

.subsection-title {
  font-size: 1rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid #e1e8ed;
  padding-bottom: 0.5rem;
}

.activities-list {
  margin: 0;
  padding-left: 1.5rem;
}

.activity-item {
  margin-bottom: 0.5rem;
  color: #34495e;
  line-height: 1.5;
}

.spots-list {
  margin: 0;
  padding-left: 1.5rem;
}

.spot-item {
  margin-bottom: 0.5rem;
  color: #34495e;
  line-height: 1.5;
}

.tips-list {
  margin: 0;
  padding-left: 1.5rem;
}

.tip-item {
  margin-bottom: 0.5rem;
  color: #34495e;
  line-height: 1.5;
}

/* 기존 스타일들 */
.json-result {
  max-height: 600px;
  overflow-y: auto;
}

.structured-result {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.result-item {
  padding: 1.5rem;
  background-color: #ffffff;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.result-item:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.item-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
  border-bottom: 2px solid #3498db;
  padding-bottom: 0.5rem;
}

.item-description {
  font-size: 1rem;
  line-height: 1.6;
  color: #34495e;
  margin: 0;
}

.json-details {
  margin-top: 1rem;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
}

.json-details summary {
  cursor: pointer;
  font-weight: 600;
  color: #3498db;
  margin-bottom: 0.5rem;
}

.json-details summary:hover {
  color: #2980b9;
}

.json-content {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  color: #2c3e50;
  white-space: pre-wrap;
  word-break: break-word;
}

.text-result {
  line-height: 1.6;
}

.result-text {
  font-size: 1rem;
  line-height: 1.8;
  color: #2c3e50;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 1rem;
  background-color: #ffffff;
  border-radius: 8px;
  border-left: 4px solid #3498db;
}

.result-text strong {
  color: #2c3e50;
  font-weight: 600;
}

.result-text br {
  margin-bottom: 0.5rem;
}

.result-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.action-button {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.copy-button {
  background-color: #95a5a6;
  color: white;
}

.copy-button:hover {
  background-color: #7f8c8d;
  transform: translateY(-2px);
}

.download-button {
  background-color: #27ae60;
  color: white;
}

.download-button:hover {
  background-color: #229954;
  transform: translateY(-2px);
}

@media (max-width: 768px) {
  .result-container {
    margin: 1rem;
    padding: 1rem;
  }

  .result-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .spots-grid {
    grid-template-columns: 1fr;
  }

  .result-actions {
    flex-direction: column;
  }

  .action-button {
    width: 100%;
    justify-content: center;
  }
  
  .plan-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .meta-item {
    font-size: 0.9rem;
  }
  
  .day-content {
    flex-direction: column;
    gap: 1rem;
  }
  
  .activities-section,
  .spots-section {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .result-container {
    margin: 0.5rem;
    padding: 0.75rem;
  }
  
  .result-title {
    font-size: 1.3rem;
  }
  
  .summary-title {
    font-size: 1.1rem;
  }
  
  .spot-card {
    padding: 0.75rem;
  }
  
  .day-plan {
    padding: 0.75rem;
  }
  
  .day-title {
    font-size: 1rem;
  }
  
  .action-button {
    padding: 0.5rem 0.75rem;
    font-size: 0.8rem;
  }
  
  .json-content {
    font-size: 0.7rem;
    padding: 0.5rem;
  }
}
</style> 