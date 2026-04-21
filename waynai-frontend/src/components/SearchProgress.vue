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
  
  // 시간 기반 프로그레스 계산
  const stepProgress: Record<string, number> = {
    'intent_analysis': 33,
    'knowledge_search': 66,
    'course_generation': 90
  };
  
  const currentStep = state.currentStep;
  if (currentStep && stepProgress[currentStep] !== undefined) {
    return stepProgress[currentStep];
  }
  
  // 기본 진행률 (단계가 설정되지 않은 경우)
  return 10;
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

</script>

<style scoped>
/* ========== SearchProgress — Material Design 3 ========== */
.progress-container {
  max-width: 640px;
  margin: 2rem auto;
  padding: 2rem;
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-xl);
  box-shadow: var(--m3-elev-1);
  color: var(--m3-on-surface);
}

.progress-header { text-align: center; margin-bottom: 1.5rem; }
.progress-title {
  font: var(--m3-title-large);
  color: var(--m3-on-surface);
  margin: 0 0 0.375rem;
}
.progress-status {
  font: var(--m3-label-large);
  color: var(--m3-primary);
}

.progress-content { margin-bottom: 1.5rem; }

.current-step { margin-bottom: 1.75rem; }
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.75rem;
}
.step-dot {
  width: 12px; height: 12px;
  border-radius: 50%;
  background: var(--m3-surface-container-highest);
  transition: background var(--m3-motion-short), transform var(--m3-motion-short);
}
.step-dot.active {
  background: var(--m3-primary);
  transform: scale(1.2);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--m3-primary) 18%, transparent);
}
.step-line {
  width: 60px; height: 2px;
  background: var(--m3-surface-container-highest);
  margin: 0 0.5rem;
  transition: background var(--m3-motion-short);
}
.step-line.active { background: var(--m3-primary); }

.step-labels {
  display: flex;
  justify-content: space-between;
  max-width: 300px;
  margin: 0 auto;
}
.step-label {
  font: var(--m3-label-medium);
  color: var(--m3-on-surface-variant);
  transition: color var(--m3-motion-short);
}
.step-label.active { color: var(--m3-primary); font-weight: 600; }

.progress-messages {
  max-height: 200px;
  overflow-y: auto;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background: var(--m3-surface-container);
  border-radius: var(--m3-shape-md);
}
.progress-message {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.375rem 0;
  animation: fadeInUp 0.3s ease;
}
.message-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px; height: 20px;
  color: var(--m3-tertiary);
}
.loading-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: var(--m3-primary);
  animation: pulse 1.5s infinite;
}
.message-text {
  font: var(--m3-body-medium);
  color: var(--m3-on-surface);
  line-height: 1.4;
}

.current-task {
  padding: 0.875rem 1rem;
  background: var(--m3-primary-container);
  color: var(--m3-on-primary-container);
  border-radius: var(--m3-shape-md);
}
.task-indicator { display: flex; align-items: center; gap: 0.75rem; }
.loading-spinner {
  width: 20px; height: 20px;
  border: 2px solid color-mix(in srgb, var(--m3-on-primary-container) 24%, transparent);
  border-top-color: var(--m3-on-primary-container);
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}
.task-text { font: var(--m3-label-large); color: inherit; }

.progress-bar-container { margin-top: 1.25rem; }
.progress-bar {
  width: 100%; height: 6px;
  background: var(--m3-surface-container-highest);
  border-radius: 999px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}
.progress-fill {
  height: 100%;
  background: var(--m3-primary);
  border-radius: 999px;
  transition: width 0.5s ease;
}
.progress-text {
  text-align: center;
  font: var(--m3-label-medium);
  color: var(--m3-on-surface-variant);
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(6px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.4; }
}
@keyframes spin {
  0%   { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .progress-container { margin: 1rem; padding: 1.25rem; }
  .step-line { width: 40px; }
  .step-labels { max-width: 260px; }
}
</style> 