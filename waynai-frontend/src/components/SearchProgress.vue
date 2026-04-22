<template>
  <div v-if="isVisible" class="progress-container">
    <header class="progress-header">
      <div class="header-eyebrow">waynai · 여행 어시스턴트</div>
      <h3 class="progress-title">{{ headerTitle }}</h3>
      <p class="progress-subtitle">{{ headerSubtitle }}</p>
    </header>

    <!-- 단계 스텝퍼 -->
    <ol class="step-list">
      <li
        v-for="(step, index) in steps"
        :key="step.id"
        class="step-item"
        :class="{
          'is-active': activeIndex === index,
          'is-complete': activeIndex > index || isCompleted,
        }"
      >
        <span class="step-dot">
          <svg v-if="activeIndex > index || isCompleted" width="14" height="14" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17Z" fill="currentColor" />
          </svg>
          <span v-else-if="activeIndex === index" class="step-dot-pulse" />
          <span v-else class="step-dot-idle" />
        </span>
        <div class="step-text">
          <strong>{{ step.label }}</strong>
          <span>{{ step.caption }}</span>
        </div>
      </li>
    </ol>

    <!-- 라이브 수집 내역 -->
    <div class="live-grid">
      <section class="live-card">
        <div class="live-head">
          <span class="live-tag tag-intent">의도 분석</span>
          <span class="live-count">
            {{ progress.intent ? '완료' : '대기 중' }}
          </span>
        </div>
        <div v-if="progress.intent" class="live-body">
          <div class="intent-row">
            <span class="intent-key">분류</span>
            <span class="intent-val">{{ progress.intent.intent || '-' }}</span>
          </div>
          <div class="intent-row">
            <span class="intent-key">지역</span>
            <span class="intent-val">
              {{ progress.intent.area?.name || '미지정' }}
              <small v-if="progress.intent.area?.sigungu?.name">· {{ progress.intent.area.sigungu.name }}</small>
            </span>
          </div>
          <div class="intent-row">
            <span class="intent-key">키워드</span>
            <span class="intent-val">{{ progress.intent.keyword || '없음' }}</span>
          </div>
        </div>
        <p v-else class="live-empty">사용자 입력을 해석하는 중입니다.</p>
      </section>

      <section class="live-card">
        <div class="live-head">
          <span class="live-tag tag-tour">관광공사 API</span>
          <span class="live-count">{{ tourCountLabel }}</span>
        </div>
        <ul v-if="progress.sources.tour?.items?.length" class="live-list">
          <li v-for="(item, i) in progress.sources.tour.items.slice(0, 5)" :key="`tour-${i}`">
            <span class="live-item-title">{{ item.title }}</span>
            <span v-if="item.subtitle" class="live-item-sub">{{ item.subtitle }}</span>
          </li>
        </ul>
        <p v-else class="live-empty">
          {{ isSearchingStage ? '관광지 정보를 수집 중...' : '아직 수집된 항목이 없습니다.' }}
        </p>
      </section>

      <section class="live-card">
        <div class="live-head">
          <span class="live-tag tag-naver">네이버 블로그</span>
          <span class="live-count">{{ naverCountLabel }}</span>
        </div>
        <ul v-if="progress.sources.naver?.items?.length" class="live-list">
          <li v-for="(item, i) in progress.sources.naver.items.slice(0, 4)" :key="`naver-${i}`">
            <a v-if="item.url" :href="item.url" target="_blank" rel="noopener" class="live-item-title">
              {{ item.title }}
            </a>
            <span v-else class="live-item-title">{{ item.title }}</span>
            <span v-if="item.subtitle" class="live-item-sub">{{ item.subtitle }}</span>
          </li>
        </ul>
        <p v-else class="live-empty">
          {{ isSearchingStage ? '블로그 리뷰를 수집 중...' : '참고 블로그가 없습니다.' }}
        </p>
      </section>

      <section class="live-card live-card--model">
        <div class="live-head">
          <span class="live-tag tag-model">AI 모델</span>
          <span class="live-count">{{ progress.model ? '선택됨' : '대기 중' }}</span>
        </div>
        <div v-if="progress.model" class="model-block">
          <div class="model-name">{{ progress.model }}</div>
          <div class="model-caption">Gemini 핫스왑 라우터 · 자동 선택</div>
        </div>
        <p v-else class="live-empty">모델이 선택되면 표시됩니다.</p>
      </section>
    </div>

    <!-- 이벤트 로그 -->
    <div class="event-log">
      <div class="event-log-head">실시간 로그</div>
      <ul class="event-log-list">
        <li v-for="(msg, idx) in recentMessages" :key="`msg-${idx}`" class="event-log-item">
          <span class="event-tag" :class="`tag-${msg.type.split('.')[0]}`">{{ tagText(msg.type) }}</span>
          <span class="event-text">{{ msg.text }}</span>
        </li>
        <li v-if="recentMessages.length === 0" class="event-log-empty">
          이벤트 수신 대기 중...
        </li>
      </ul>
    </div>

    <!-- 진행률 -->
    <div class="progress-bar-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: progressPercentage + '%' }" />
      </div>
      <div class="progress-text">{{ stageLabel }} · {{ progressPercentage }}%</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useStreamStore, type TravelStage } from '@/stores/stream';

const streamStore = useStreamStore();
const progress = computed(() => streamStore.state.progress);
const isStreaming = computed(() => streamStore.state.isStreaming);
const isComplete = computed(() => streamStore.state.isComplete);
const hasError = computed(() => !!streamStore.state.error);

const isVisible = computed(() =>
  isStreaming.value ||
  progress.value.stage !== 'idle' ||
  progress.value.messages.length > 0
);

const steps = [
  { id: 'analyzing', label: '입력 분석', caption: '사용자 의도와 지역/키워드 파악' },
  { id: 'searching', label: '컨텍스트 수집', caption: '관광공사 + 네이버 블로그 병렬 RAG' },
  { id: 'generating', label: 'AI 생성', caption: 'Gemini 모델이 여행 계획을 작성' },
] as const;

const STAGE_ORDER: Record<TravelStage, number> = {
  idle: -1,
  analyzing: 0,
  searching: 1,
  generating: 2,
  completed: 3,
  error: -1,
};

const activeIndex = computed(() => STAGE_ORDER[progress.value.stage] ?? -1);
const isCompleted = computed(() => progress.value.stage === 'completed' || isComplete.value);
const isSearchingStage = computed(() => progress.value.stage === 'searching');

const progressPercentage = computed(() => {
  if (hasError.value) return 100;
  const idx = activeIndex.value;
  if (idx < 0) return 5;
  if (progress.value.stage === 'completed') return 100;
  // 단계별 베이스 + sources/model 수신에 따른 가중
  const base = [20, 45, 75][idx] ?? 85;
  let bonus = 0;
  if (progress.value.intent) bonus += 5;
  if (progress.value.sources.tour) bonus += 5;
  if (progress.value.sources.naver) bonus += 5;
  if (progress.value.model) bonus += 5;
  return Math.min(98, base + bonus);
});

const tourCountLabel = computed(() => {
  const s = progress.value.sources.tour;
  if (!s) return '대기 중';
  return `${s.count}건`;
});

const naverCountLabel = computed(() => {
  const s = progress.value.sources.naver;
  if (!s) return '대기 중';
  return `${s.count}건`;
});

const headerTitle = computed(() => {
  if (hasError.value) return '오류가 발생했어요';
  if (isCompleted.value) return '여행 계획이 준비됐어요';
  if (progress.value.stage === 'generating') return 'AI가 이야기를 엮고 있어요';
  if (progress.value.stage === 'searching') return '컨텍스트를 모으고 있어요';
  if (progress.value.stage === 'analyzing') return '여행 의도를 들여다보는 중';
  return '여행 어시스턴트 준비';
});

const headerSubtitle = computed(() => {
  if (hasError.value) return streamStore.state.error || '잠시 후 다시 시도해 주세요.';
  if (progress.value.model) return `사용 모델: ${progress.value.model}`;
  return 'waynai 가 당신의 질의를 실시간으로 해석하고 있어요.';
});

const stageLabel = computed(() => {
  switch (progress.value.stage) {
    case 'analyzing': return '입력 분석 중';
    case 'searching': return '컨텍스트 수집 중';
    case 'generating': return 'AI 생성 중';
    case 'completed': return '완료';
    case 'error': return '오류';
    default: return '대기 중';
  }
});

const recentMessages = computed(() => {
  const msgs = progress.value.messages;
  // 최근 8개만 노출. 시간 역순(최근이 위)로 보여주기.
  return [...msgs].slice(-8).reverse();
});

const tagText = (type: string): string => {
  const map: Record<string, string> = {
    stage: '단계',
    intent: '의도',
    'sources.tour': '관광',
    'sources.naver': '블로그',
    model: '모델',
    plan: '계획',
    done: '완료',
    error: '오류',
  };
  return map[type] ?? type;
};
</script>

<style scoped>
.progress-container {
  max-width: 960px;
  margin: 2rem auto;
  padding: clamp(1.5rem, 2.5vw, 2rem);
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 28px;
  box-shadow: 0 24px 60px -32px color-mix(in srgb, var(--wa-ocean) 35%, transparent);
  color: var(--wa-text-dark);
}

.progress-header {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-bottom: 1.75rem;
}
.header-eyebrow {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--wa-terra);
}
.progress-title {
  font-family: var(--wa-font-serif);
  font-size: clamp(1.5rem, 1.5vw + 1rem, 2rem);
  font-weight: 500;
  letter-spacing: -0.01em;
  color: var(--wa-ocean);
  margin: 0;
  font-style: italic;
}
.progress-subtitle {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  margin: 0;
}

.step-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
  list-style: none;
  padding: 0;
  margin: 0 0 1.75rem;
}
.step-item {
  display: flex;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  background: color-mix(in srgb, var(--wa-sand) 40%, var(--wa-cream));
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 60%, transparent);
  transition: background 180ms ease, border-color 180ms ease;
}
.step-item.is-active {
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-color: transparent;
  box-shadow: 0 12px 30px -16px color-mix(in srgb, var(--wa-ocean) 60%, transparent);
}
.step-item.is-complete {
  background: var(--wa-sage);
  color: var(--wa-cream);
  border-color: transparent;
}
.step-dot {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: color-mix(in srgb, currentColor 12%, transparent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: inherit;
}
.step-item.is-active .step-dot { background: var(--wa-amber); color: var(--wa-ocean); }
.step-item.is-complete .step-dot { background: var(--wa-cream); color: var(--wa-sage-dark); }
.step-dot-pulse,
.step-dot-idle {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
}
.step-dot-pulse { animation: pulse 1.4s infinite; }
.step-dot-idle { opacity: 0.4; }
.step-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.step-text strong {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  font-weight: 600;
  color: inherit;
}
.step-text span {
  font-size: 0.75rem;
  color: inherit;
  opacity: 0.75;
  letter-spacing: 0.01em;
}

.live-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}
.live-card {
  background: var(--wa-warm);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 18px;
  padding: 1rem 1.125rem;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  min-height: 120px;
}
.live-card--model {
  background: linear-gradient(135deg, var(--wa-ocean), var(--wa-dusk));
  color: var(--wa-cream);
  border-color: transparent;
}

.live-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.live-tag {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  padding: 0.25rem 0.625rem;
  border-radius: 999px;
  color: var(--wa-cream);
}
.tag-intent { background: var(--wa-sage-dark); }
.tag-tour   { background: var(--wa-terra); }
.tag-naver  { background: #3c6b72; }
.tag-model  { background: var(--wa-amber); color: var(--wa-ocean); }
.tag-stage  { background: var(--wa-ocean); }
.tag-plan   { background: var(--wa-sage); }
.tag-done   { background: var(--wa-sage-dark); }
.tag-error  { background: #b14a4a; }

.live-count {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: inherit;
  opacity: 0.8;
}

.live-body {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}
.intent-row {
  display: flex;
  gap: 0.5rem;
  font-size: 0.8125rem;
}
.intent-key {
  min-width: 44px;
  color: var(--wa-text-light);
  font-weight: 500;
}
.intent-val {
  color: var(--wa-text-dark);
  font-weight: 500;
}
.intent-val small { color: var(--wa-text-light); font-weight: 400; margin-left: 4px; }

.live-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.live-list li {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.35;
  margin: 0;
}
.live-item-title {
  font-size: 0.8125rem;
  color: var(--wa-text-dark);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
a.live-item-title { color: var(--wa-dusk); }
a.live-item-title:hover { color: var(--wa-terra); text-decoration: underline; }
.live-item-sub {
  font-size: 0.6875rem;
  color: var(--wa-text-light);
}
.live-empty {
  font-size: 0.8125rem;
  color: color-mix(in srgb, currentColor 65%, transparent);
  font-style: italic;
  margin: 0;
}

.model-block { display: flex; flex-direction: column; gap: 4px; }
.model-name {
  font-family: var(--wa-font-serif);
  font-size: 1.25rem;
  font-style: italic;
  color: var(--wa-amber);
}
.model-caption {
  font-size: 0.75rem;
  color: color-mix(in srgb, var(--wa-cream) 75%, transparent);
  letter-spacing: 0.02em;
}

.event-log {
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-radius: 20px;
  padding: 1rem 1.25rem;
  margin-bottom: 1.25rem;
}
.event-log-head {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--wa-amber);
  margin-bottom: 0.75rem;
}
.event-log-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 180px;
  overflow-y: auto;
}
.event-log-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  font-size: 0.8125rem;
  line-height: 1.45;
  margin: 0;
  color: color-mix(in srgb, var(--wa-cream) 92%, transparent);
}
.event-tag {
  flex-shrink: 0;
  font-size: 0.625rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 600;
  padding: 0.1875rem 0.5rem;
  border-radius: 999px;
  color: var(--wa-cream);
  background: color-mix(in srgb, var(--wa-cream) 18%, transparent);
  line-height: 1.4;
}
.event-log-empty {
  font-style: italic;
  opacity: 0.6;
  font-size: 0.8125rem;
  margin: 0;
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 1rem;
}
.progress-bar {
  flex: 1;
  height: 6px;
  background: color-mix(in srgb, var(--wa-sand) 75%, transparent);
  border-radius: 999px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--wa-sage) 0%, var(--wa-terra) 100%);
  border-radius: inherit;
  transition: width 400ms ease;
}
.progress-text {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-mid);
  letter-spacing: 0.04em;
  text-transform: uppercase;
  white-space: nowrap;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%      { opacity: 0.45; transform: scale(0.7); }
}

@media (max-width: 720px) {
  .progress-container { padding: 1.25rem; border-radius: 20px; }
  .step-list { grid-template-columns: 1fr; }
}
</style>
