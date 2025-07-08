<template>
  <div v-if="state.result || state.error" class="result-container">
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

    <div v-else-if="state.result" class="success-result">
      <div class="result-content">
        <div class="result-type-indicator">
          <span class="type-badge" :class="resultTypeClass">
            {{ getResultTypeText() }}
          </span>
        </div>

        <div class="result-body">
          <div v-if="isJsonResult" class="json-result">
            <pre class="json-content">{{ formatJson(state.result) }}</pre>
          </div>
          <div v-else class="text-result">
            <div class="result-text">{{ state.result }}</div>
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

const isJsonResult = computed(() => {
  if (typeof state.result === 'string') {
    try {
      JSON.parse(state.result);
      return true;
    } catch {
      return false;
    }
  }
  return typeof state.result === 'object';
});

const resultTypeClass = computed(() => {
  // 의도 분석 결과에 따라 클래스 결정
  if (state.result && typeof state.result === 'string') {
    if (state.result.includes('추천') || state.result.includes('관광지')) {
      return 'type-recommendation';
    } else if (state.result.includes('계획') || state.result.includes('일정')) {
      return 'type-plan';
    }
  }
  return 'type-general';
});

const getResultTypeText = () => {
  if (state.result && typeof state.result === 'string') {
    if (state.result.includes('추천') || state.result.includes('관광지')) {
      return '관광지 추천';
    } else if (state.result.includes('계획') || state.result.includes('일정')) {
      return '여행 계획';
    }
  }
  return '검색 결과';
};

const formatJson = (data: any) => {
  try {
    if (typeof data === 'string') {
      return JSON.stringify(JSON.parse(data), null, 2);
    }
    return JSON.stringify(data, null, 2);
  } catch {
    return data;
  }
};

const handleCopy = async () => {
  try {
    const textToCopy = typeof state.result === 'string' 
      ? state.result 
      : JSON.stringify(state.result, null, 2);
    
    await navigator.clipboard.writeText(textToCopy);
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

.json-result {
  max-height: 400px;
  overflow-y: auto;
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
  color: #2c3e50;
  font-size: 1rem;
  white-space: pre-wrap;
  word-break: break-word;
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

  .result-actions {
    flex-direction: column;
  }

  .action-button {
    width: 100%;
    justify-content: center;
  }
}
</style> 