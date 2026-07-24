<template>
  <div v-if="isVisible" class="finder">
    <header class="finder-head">
      <span class="finder-eyebrow">여행 플래너</span>
      <h3 class="finder-title">{{ headerTitle }}</h3>
      <p class="finder-sub">{{ headerSubtitle }}</p>
    </header>

    <!-- 이해한 여행 요약 (개발 용어 없이 사람 언어로) -->
    <div v-if="understood.length" class="understood">
      <span v-for="(u, i) in understood" :key="i" class="understood-chip">
        <span class="uc-key">{{ u.key }}</span>
        <span class="uc-val">{{ u.val }}</span>
      </span>
    </div>

    <!-- 진행 단계 -->
    <ol class="steps">
      <li
        v-for="(step, index) in steps"
        :key="step.id"
        class="step"
        :class="{ active: activeIndex === index && !isCompleted, done: activeIndex > index || isCompleted }"
      >
        <span class="step-icon">
          <svg v-if="activeIndex > index || isCompleted" width="14" height="14" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17Z" fill="currentColor" />
          </svg>
          <span v-else-if="activeIndex === index" class="spin-ring" />
          <span v-else class="idle-dot" />
        </span>
        <div class="step-text">
          <strong>{{ step.label }}</strong>
          <span>{{ activeIndex === index && !isCompleted ? step.doing : step.caption }}</span>
        </div>
      </li>
    </ol>

    <!-- 항공권을 찾았을 때만 친근하게 노출 -->
    <div v-if="cheapestFlight" class="finder-flight">
      <span class="ff-plane">✈️</span>
      <span class="ff-text">
        {{ cheapestFlight.origin }} → {{ cheapestFlight.destination }} 최저가
        <strong>{{ flightPrice }}</strong> ({{ cheapestFlight.roundTrip ? '왕복' : '편도' }}) 찾음
      </span>
    </div>

    <div v-if="topHotel" class="finder-flight">
      <span class="ff-plane">🏨</span>
      <span class="ff-text">
        숙소 <strong>{{ topHotel.name }}</strong>
        <template v-if="topHotel.pricePerNightKrw"> · 1박 {{ topHotel.pricePerNightKrw.toLocaleString('ko-KR') }}원~</template> 찾음
      </span>
    </div>

    <!-- 수집한 참고 정보(볼거리·블로그)를 대기 중에 노출 -->
    <div v-if="collected.length" class="finder-collected">
      <span class="fc-label">참고하는 정보</span>
      <div class="fc-list">
        <span v-for="(c, i) in collected" :key="i" class="fc-item">
          <span class="fc-ic">{{ c.ic }}</span>{{ c.text }}
        </span>
      </div>
    </div>

    <div class="finder-bar">
      <div class="finder-bar-track"><div class="finder-bar-fill" :style="{ width: pct + '%' }" /></div>
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
const flights = computed(() => streamStore.state.flights || []);
const topHotel = computed(() => (streamStore.state.hotels || [])[0] || null);

const isVisible = computed(() =>
  isStreaming.value || progress.value.stage !== 'idle' || progress.value.messages.length > 0,
);

const steps = [
  { id: 'analyzing', label: '여행 요청 이해하기', caption: '원하시는 여행을 파악해요', doing: '요청을 읽는 중…' },
  { id: 'searching', label: '가는 길·볼거리 찾기', caption: '항공편과 가볼 만한 곳을 모아요', doing: '항공편·명소를 찾는 중…' },
  { id: 'generating', label: '일정 완성하기', caption: '동선과 비용까지 맞춘 일정으로', doing: '일정을 짜는 중…' },
] as const;

const STAGE_ORDER: Record<TravelStage, number> = {
  idle: -1, analyzing: 0, searching: 1, generating: 2, completed: 3, error: -1,
};
const activeIndex = computed(() => STAGE_ORDER[progress.value.stage] ?? -1);
const isCompleted = computed(() => progress.value.stage === 'completed' || isComplete.value);

const pct = computed(() => {
  if (hasError.value) return 100;
  if (isCompleted.value) return 100;
  const idx = activeIndex.value;
  if (idx < 0) return 6;
  const base = [25, 55, 80][idx] ?? 85;
  let bonus = 0;
  if (progress.value.intent) bonus += 6;
  if (flights.value.length) bonus += 6;
  return Math.min(96, base + bonus);
});

// 이해한 조건을 사람 언어로 (코드/JSON 아님)
const understood = computed(() => {
  const i = progress.value.intent;
  if (!i) return [] as { key: string; val: string }[];
  const out: { key: string; val: string }[] = [];
  const dest = i.destination || i.area?.name;
  if (dest) out.push({ key: '목적지', val: dest });
  if (i.days) out.push({ key: '기간', val: `${i.days}일` });
  if (i.origin) out.push({ key: '출발', val: i.origin });
  if (i.departDate) out.push({ key: '출발일', val: i.departDate });
  if (i.style) out.push({ key: '스타일', val: i.style });
  if (i.companions) out.push({ key: '동반', val: i.companions });
  if (i.budgetLevel) out.push({ key: '예산', val: i.budgetLevel });
  if (i.keyword) out.push({ key: '관심사', val: i.keyword });
  return out;
});

const cheapestFlight = computed(() => {
  if (!flights.value.length) return null;
  return [...flights.value].sort((a, b) => (a.price ?? Infinity) - (b.price ?? Infinity))[0];
});

// 수집된 관광지·블로그를 사람이 읽는 형태로 (개발 용어/출처명 없이).
const collected = computed(() => {
  const out: { ic: string; text: string }[] = [];
  const tour = progress.value.sources.tour;
  const naver = progress.value.sources.naver;
  const web = progress.value.sources.web;
  (web?.items || []).slice(0, 5).forEach((it) => { if (it.title) out.push({ ic: '🌐', text: it.title }); });
  (tour?.items || []).slice(0, 4).forEach((it) => { if (it.title) out.push({ ic: '📍', text: it.title }); });
  (naver?.items || []).slice(0, 4).forEach((it) => { if (it.title) out.push({ ic: '📝', text: it.title }); });
  return out;
});
const flightPrice = computed(() => {
  const f = cheapestFlight.value;
  if (!f || typeof f.price !== 'number') return '';
  return `${f.price.toLocaleString('ko-KR')} ${(f.currency || 'KRW').toUpperCase()}`;
});

const headerTitle = computed(() => {
  if (hasError.value) return '잠시 문제가 생겼어요';
  if (isCompleted.value) return '여행 계획이 준비됐어요';
  if (progress.value.stage === 'generating') return '일정을 짜고 있어요';
  if (progress.value.stage === 'searching') return '가는 길과 볼거리를 찾고 있어요';
  if (progress.value.stage === 'analyzing') return '요청을 이해하고 있어요';
  return '여행 계획을 준비할게요';
});
const headerSubtitle = computed(() => {
  if (hasError.value) return streamStore.state.error || '잠시 후 다시 시도해 주세요.';
  if (isCompleted.value) return '아래에서 완성된 일정을 확인하세요.';
  return '실제 정보를 바탕으로 맞춤 여행을 찾고 있어요.';
});
</script>

<style scoped>
.finder {
  max-width: 960px;
  margin: 1.5rem auto;
  padding: clamp(1.25rem, 2.5vw, 1.75rem);
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 24px;
  box-shadow: 0 24px 60px -34px color-mix(in srgb, var(--wa-ocean) 32%, transparent);
  color: var(--wa-text-dark);
}
.finder-head { display: flex; flex-direction: column; gap: 0.3rem; margin-bottom: 1.25rem; }
.finder-eyebrow {
  font-size: 0.72rem; font-weight: 600; letter-spacing: 0.16em; text-transform: uppercase;
  color: var(--wa-terra);
}
.finder-title {
  font-family: var(--wa-font-serif); font-style: italic; font-size: clamp(1.35rem, 1.4vw + 1rem, 1.9rem);
  color: var(--wa-ocean); margin: 0;
}
.finder-sub { font-size: 0.9rem; color: var(--wa-text-mid); margin: 0; }

.understood { display: flex; flex-wrap: wrap; gap: 0.5rem; margin-bottom: 1.25rem; }
.understood-chip {
  display: inline-flex; align-items: center; gap: 0.4rem;
  background: color-mix(in srgb, var(--wa-sand) 40%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-sand) 60%, transparent);
  border-radius: 999px; padding: 0.3rem 0.75rem; font-size: 0.82rem;
}
.uc-key { color: var(--wa-text-light); font-size: 0.72rem; }
.uc-val { color: var(--wa-ocean); font-weight: 600; }

.steps { list-style: none; padding: 0; margin: 0 0 1.25rem; display: flex; flex-direction: column; gap: 0.6rem; }
.step {
  display: flex; gap: 0.85rem; align-items: center; padding: 0.8rem 1rem;
  background: color-mix(in srgb, var(--wa-sand) 32%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 14px; transition: all 200ms ease;
}
.step.active { background: var(--wa-ocean); color: var(--wa-cream); border-color: transparent; }
.step.done { background: color-mix(in srgb, var(--wa-sage) 22%, var(--wa-cream)); border-color: transparent; }
.step-icon {
  flex-shrink: 0; width: 30px; height: 30px; border-radius: 50%;
  display: inline-flex; align-items: center; justify-content: center;
  background: color-mix(in srgb, currentColor 14%, transparent);
}
.step.active .step-icon { background: var(--wa-amber); color: var(--wa-ocean); }
.step.done .step-icon { background: var(--wa-sage); color: #fff; }
.spin-ring {
  width: 15px; height: 15px; border-radius: 50%;
  border: 2px solid color-mix(in srgb, currentColor 35%, transparent); border-top-color: currentColor;
  animation: fspin 0.8s linear infinite;
}
@keyframes fspin { to { transform: rotate(360deg); } }
.idle-dot { width: 9px; height: 9px; border-radius: 50%; background: currentColor; opacity: 0.35; }
.step-text { display: flex; flex-direction: column; gap: 1px; min-width: 0; }
.step-text strong { font-size: 0.92rem; font-weight: 600; }
.step-text span { font-size: 0.76rem; opacity: 0.78; }

.finder-flight {
  display: flex; align-items: center; gap: 0.6rem; margin-bottom: 1rem;
  padding: 0.7rem 1rem; border-radius: 12px;
  background: color-mix(in srgb, var(--wa-amber) 16%, var(--wa-cream));
  font-size: 0.88rem; color: var(--wa-text-dark);
}
.ff-text strong { color: var(--wa-terra); }

.finder-bar-track {
  height: 6px; background: color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 999px; overflow: hidden;
}
.finder-bar-fill {
  height: 100%; border-radius: inherit;
  background: linear-gradient(90deg, var(--wa-sage), var(--wa-terra));
  transition: width 400ms ease;
}
</style>

<style scoped>
.finder-collected { margin-bottom: 1rem; }
.fc-label { font-size: 0.72rem; font-weight: 600; letter-spacing: 0.1em; text-transform: uppercase; color: var(--wa-text-light, #999); }
.fc-list { display: flex; flex-wrap: wrap; gap: 0.4rem; margin-top: 0.5rem; }
.fc-item {
  display: inline-flex; align-items: center; gap: 0.35rem;
  background: color-mix(in srgb, var(--wa-sand) 26%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-sand) 50%, transparent);
  border-radius: 999px; padding: 0.28rem 0.7rem; font-size: 0.8rem; color: var(--wa-text-dark, #333);
  animation: fc-in 240ms ease;
}
@keyframes fc-in { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: none; } }
.fc-ic { font-size: 0.85rem; }
</style>
