<template>
  <div v-if="state.isSearching" class="progress-container">
    <div class="progress-header">
      <h3 class="progress-title">AI 여행 코스 생성 중</h3>
      <div class="progress-status">{{ getStatusText() }}</div>
    </div>

    <div class="progress-content">
      <!-- 현재 단계 표시 -->
      <div class="current-step">
        <div class="step-indicator">
          <div class="step-dot active"></div>
          <div class="step-line" :class="{ active: isStepActive('intent_analysis') }"></div>
          <div class="step-dot" :class="{ active: isStepActive('intent_analysis') }"></div>
          <div class="step-line" :class="{ active: isStepActive('knowledge_search') }"></div>
          <div class="step-dot" :class="{ active: isStepActive('knowledge_search') }"></div>
          <div class="step-line" :class="{ active: isStepActive('course_generation') }"></div>
          <div class="step-dot" :class="{ active: isStepActive('course_generation') }"></div>
        </div>
        
        <div class="step-labels">
          <span class="step-label" :class="{ active: isStepActive('intent_analysis') }">입력 분석</span>
          <span class="step-label" :class="{ active: isStepActive('knowledge_search') }">정보 검색</span>
          <span class="step-label" :class="{ active: isStepActive('course_generation') }">AI 생성</span>
        </div>
      </div>

      <!-- 진행 상황 메시지 -->
      <div class="progress-messages">
        <div v-for="(message, index) in state.progress" :key="index" class="progress-message">
          <div class="message-icon">
            <svg v-if="isCompletedMessage(message)" width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z" fill="currentColor"/>
            </svg>
            <div v-else class="loading-dot"></div>
          </div>
          <span class="message-text">{{ message }}</span>
        </div>
      </div>

      <!-- 현재 진행 중인 작업 표시 -->
      <div v-if="state.currentStep && !isCompleted" class="current-task">
        <div class="task-indicator">
          <div class="loading-spinner"></div>
          <span class="task-text">{{ getCurrentTaskText() }}</span>
        </div>
      </div>

      <!-- 디버그 정보 (개발 모드에서만 표시) -->
      <div v-if="showDebugInfo" class="debug-info">
        <details>
          <summary>디버그 정보</summary>
          <pre>{{ debugInfo }}</pre>
        </details>
      </div>
    </div>

    <!-- 진행률 표시 -->
    <div class="progress-bar-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercentage + '%' }"></div>
      </div>
      <div class="progress-text">{{ Math.round(progressPercentage) }}% 완료</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useSearchStore } from '@/stores/search';

const searchStore = useSearchStore();
const { state } = searchStore;

const isCompleted = computed(() => {
  return state.currentStatus === 'completed';
});

const progressPercentage = computed(() => {
  if (isCompleted.value) return 100;
  
  const stepProgress: Record<string, number> = {
    'intent_analysis': 0,
    'knowledge_search': 33,
    'course_generation': 66
  };
  
  const currentStep = state.currentStep;
  if (currentStep && stepProgress[currentStep] !== undefined) {
    return stepProgress[currentStep];
  }
  
  return 0;
});

const isStepActive = (step: string) => {
  const stepOrder = ['intent_analysis', 'knowledge_search', 'course_generation'];
  const currentStepIndex = stepOrder.indexOf(state.currentStep);
  const stepIndex = stepOrder.indexOf(step);
  
  return stepIndex <= currentStepIndex;
};

const isCompletedMessage = (message: string) => {
  return message.includes('완료') || message.includes('완료:');
};

const getCurrentTaskText = () => {
  const taskTexts: Record<string, string> = {
    'intent_analysis': '사용자 입력을 분석하고 질의를 강화하는 중...',
    'knowledge_search': '관광지 정보를 검색하고 수집하는 중...',
    'course_generation': 'AI가 여행 정보를 생성하는 중...'
  };
  
  return taskTexts[state.currentStep] || '처리 중...';
};

const getStatusText = () => {
  const statusTexts: Record<string, string> = {
    'analyzing': '입력 분석 중',
    'searching': '정보 검색 중',
    'generating': 'AI 생성 중',
    'completed': '완료',
    'error': '오류 발생'
  };
  
  return statusTexts[state.currentStatus] || state.currentStatus || '대기 중';
};

// 디버그 정보 (개발 모드에서만 표시)
const showDebugInfo = computed(() => {
  return import.meta.env.DEV;
});

const debugInfo = computed(() => {
  return JSON.stringify({
    isSearching: state.isSearching,
    currentStatus: state.currentStatus,
    currentStep: state.currentStep,
    progress: state.progress,
    hasResult: !!state.result,
    hasError: !!state.error
  }, null, 2);
});
</script>

<style scoped>
.progress-container {
  max-width: 600px;
  margin: 2rem auto;
  padding: 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.progress-header {
  text-align: center;
  margin-bottom: 2rem;
}

.progress-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
}

.progress-status {
  font-size: 1rem;
  color: #3498db;
  font-weight: 500;
}

.progress-content {
  margin-bottom: 2rem;
}

/* 단계 표시 */
.current-step {
  margin-bottom: 2rem;
}

.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #e1e8ed;
  transition: all 0.3s ease;
}

.step-dot.active {
  background-color: #3498db;
  transform: scale(1.2);
}

.step-line {
  width: 60px;
  height: 2px;
  background-color: #e1e8ed;
  margin: 0 0.5rem;
  transition: all 0.3s ease;
}

.step-line.active {
  background-color: #3498db;
}

.step-labels {
  display: flex;
  justify-content: space-between;
  max-width: 300px;
  margin: 0 auto;
}

.step-label {
  font-size: 0.9rem;
  color: #7f8c8d;
  font-weight: 500;
  transition: all 0.3s ease;
}

.step-label.active {
  color: #3498db;
  font-weight: 600;
}

/* 진행 메시지 */
.progress-messages {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 1.5rem;
}

.progress-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.5rem 0;
  animation: fadeInUp 0.3s ease;
}

.message-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #27ae60;
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #3498db;
  animation: pulse 1.5s infinite;
}

.message-text {
  font-size: 0.95rem;
  color: #34495e;
  line-height: 1.4;
}

/* 현재 작업 표시 */
.current-task {
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #3498db;
}

.task-indicator {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e1e8ed;
  border-top: 2px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.task-text {
  font-size: 1rem;
  color: #2c3e50;
  font-weight: 500;
}

/* 디버그 정보 */
.debug-info {
  margin-top: 1rem;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e1e8ed;
}

.debug-info summary {
  cursor: pointer;
  font-weight: 600;
  color: #3498db;
  margin-bottom: 0.5rem;
}

.debug-info pre {
  margin: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.8rem;
  line-height: 1.4;
  color: #2c3e50;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 진행률 바 */
.progress-bar-container {
  margin-top: 1.5rem;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background-color: #e1e8ed;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3498db, #2980b9);
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-text {
  text-align: center;
  font-size: 0.9rem;
  color: #7f8c8d;
  font-weight: 500;
}

/* 애니메이션 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

/* 반응형 */
@media (max-width: 768px) {
  .progress-container {
    margin: 1rem;
    padding: 1rem;
  }

  .step-line {
    width: 40px;
  }

  .step-labels {
    max-width: 250px;
  }

  .step-label {
    font-size: 0.8rem;
  }
}
</style> 