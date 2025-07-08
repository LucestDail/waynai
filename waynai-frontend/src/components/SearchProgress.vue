<template>
  <div v-if="state.isSearching || state.progress.length > 0" class="progress-container">
    <div class="progress-header">
      <h3 class="progress-title">검색 진행 상황</h3>
      <div class="status-indicator" :class="statusClass">
        {{ getStatusText(state.currentStatus) }}
      </div>
    </div>

    <div class="progress-steps">
      <div
        v-for="(step, index) in progressSteps"
        :key="step.key"
        class="progress-step"
        :class="getStepClass(step.key)"
      >
        <div class="step-icon">
          <div v-if="isStepCompleted(step.key)" class="step-completed">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z" fill="currentColor"/>
            </svg>
          </div>
          <div v-else-if="isStepActive(step.key)" class="step-active">
            <div class="step-spinner"></div>
          </div>
          <div v-else class="step-pending">
            {{ index + 1 }}
          </div>
        </div>
        <div class="step-content">
          <div class="step-title">{{ step.title }}</div>
          <div class="step-description">{{ step.description }}</div>
        </div>
      </div>
    </div>

    <div v-if="state.progress.length > 0" class="progress-log">
      <h4 class="log-title">진행 로그</h4>
      <div class="log-container">
        <div
          v-for="(log, index) in state.progress"
          :key="index"
          class="log-item"
        >
          <span class="log-time">{{ formatTime(new Date()) }}</span>
          <span class="log-message">{{ log }}</span>
        </div>
      </div>
    </div>

    <div v-if="state.error" class="error-message">
      <div class="error-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" fill="currentColor"/>
        </svg>
      </div>
      <div class="error-text">{{ state.error }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSearchStore } from '@/stores/search';

const searchStore = useSearchStore();
const { state } = searchStore;

const progressSteps = [
  {
    key: 'intent_analysis',
    title: '의도 분석',
    description: '사용자 입력을 분석하여 검색 의도를 파악합니다.'
  },
  {
    key: 'query_enhancement',
    title: '질의 강화',
    description: '원본 질의를 더 구체적이고 정확한 검색어로 강화합니다.'
  },
  {
    key: 'knowledge_search',
    title: '지식 검색',
    description: '관광지 정보를 검색하여 관련 데이터를 수집합니다.'
  },
  {
    key: 'course_generation',
    title: '결과 생성',
    description: '수집된 정보를 바탕으로 여행 계획을 생성합니다.'
  }
];

const statusClass = computed(() => {
  switch (state.currentStatus) {
    case 'analyzing':
      return 'status-analyzing';
    case 'enhancing':
      return 'status-enhancing';
    case 'searching':
      return 'status-searching';
    case 'generating':
      return 'status-generating';
    case 'completed':
      return 'status-completed';
    case 'error':
      return 'status-error';
    default:
      return '';
  }
});

const getStatusText = (status: string) => {
  switch (status) {
    case 'analyzing':
      return '의도 분석 중';
    case 'enhancing':
      return '질의 강화 중';
    case 'searching':
      return '지식 검색 중';
    case 'generating':
      return '결과 생성 중';
    case 'completed':
      return '완료';
    case 'error':
      return '오류 발생';
    default:
      return '대기 중';
  }
};

const getStepClass = (stepKey: string) => {
  if (isStepCompleted(stepKey)) return 'step-completed';
  if (isStepActive(stepKey)) return 'step-active';
  return 'step-pending';
};

const isStepCompleted = (stepKey: string) => {
  const stepOrder = ['intent_analysis', 'query_enhancement', 'knowledge_search', 'course_generation'];
  const currentIndex = stepOrder.indexOf(state.currentStep);
  const stepIndex = stepOrder.indexOf(stepKey);
  return stepIndex < currentIndex || state.currentStatus === 'completed';
};

const isStepActive = (stepKey: string) => {
  return state.currentStep === stepKey && state.currentStatus !== 'completed';
};

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};
</script>

<style scoped>
.progress-container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f1f3f4;
}

.progress-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.status-indicator {
  padding: 0.5rem 1rem;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: 600;
  color: white;
}

.status-analyzing {
  background-color: #3498db;
}

.status-enhancing {
  background-color: #9b59b6;
}

.status-searching {
  background-color: #f39c12;
}

.status-generating {
  background-color: #e67e22;
}

.status-completed {
  background-color: #27ae60;
}

.status-error {
  background-color: #e74c3c;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.progress-step {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.step-completed {
  background-color: #f8f9fa;
  border-left: 4px solid #27ae60;
}

.step-active {
  background-color: #e3f2fd;
  border-left: 4px solid #3498db;
}

.step-pending {
  background-color: #f8f9fa;
  border-left: 4px solid #bdc3c7;
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.step-completed .step-icon {
  background-color: #27ae60;
  color: white;
}

.step-active .step-icon {
  background-color: #3498db;
  color: white;
}

.step-pending .step-icon {
  background-color: #bdc3c7;
  color: #7f8c8d;
}

.step-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.step-content {
  flex: 1;
}

.step-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.25rem;
}

.step-description {
  color: #7f8c8d;
  font-size: 0.9rem;
}

.progress-log {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 2px solid #f1f3f4;
}

.log-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 1rem;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 1rem;
}

.log-item {
  display: flex;
  gap: 1rem;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
}

.log-time {
  color: #7f8c8d;
  font-weight: 600;
  min-width: 80px;
}

.log-message {
  color: #2c3e50;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
  padding: 1rem;
  background-color: #fdf2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
}

.error-icon {
  flex-shrink: 0;
}

.error-text {
  font-weight: 500;
}

@media (max-width: 768px) {
  .progress-container {
    margin: 1rem;
    padding: 1rem;
  }

  .progress-header {
    flex-direction: column;
    gap: 1rem;
    align-items: flex-start;
  }

  .progress-step {
    flex-direction: column;
    align-items: flex-start;
  }

  .step-icon {
    align-self: flex-start;
  }
}
</style> 