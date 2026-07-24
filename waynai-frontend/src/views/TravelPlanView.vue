<template>
  <div class="travel-plan-view">
    <section class="intro">
      <span class="eyebrow">세부 계획</span>
      <h1 class="title">
        원하는 여행을 <em>편하게 말해주세요</em>
      </h1>
      <p class="subtitle">
        목적지·일정·동행·예산을 한 번에 문장으로 적어주세요. 나머지는 알아서 이해하고
        항공권·동선·비용까지 맞춰 드립니다.
      </p>
    </section>

    <section class="plan-card">
      <form class="plan-form" @submit.prevent="generateTravelPlan">
        <label class="nl-label" for="nl-input">어떤 여행을 원하세요?</label>
        <textarea
          id="nl-input"
          v-model="planText"
          class="nl-input"
          rows="4"
          placeholder="예) 오사카로 3박 4일 커플 여행 가고 싶어요. 맛집이랑 쇼핑 위주로, 예산은 넉넉하게. 9월 출발 예정이에요."
          :disabled="streamState.isStreaming"
        ></textarea>

        <div class="quick-add">
          <span class="qa-label">빠른 추가</span>
          <div class="qa-groups">
            <div v-for="group in quickGroups" :key="group.title" class="qa-group">
              <span class="qa-group-title">{{ group.title }}</span>
              <div class="qa-chips">
                <button
                  v-for="chip in group.chips"
                  :key="chip"
                  type="button"
                  class="qa-chip"
                  :disabled="streamState.isStreaming"
                  @click="appendChip(chip)"
                >{{ chip }}</button>
              </div>
            </div>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="submit"
            class="submit-button"
            :disabled="!isFormValid || streamState.isStreaming"
          >
            <span v-if="streamState.isStreaming" class="spinner"></span>
            <span v-else>여행 계획 만들기</span>
          </button>
          <p class="hint">문장 한 줄이면 충분해요. 자세히 적을수록 더 정확해집니다.</p>
        </div>
      </form>
    </section>

    <section v-if="historyItems.length" class="saved-strip">
      <h3 class="saved-title">저장한 여행</h3>
      <div class="saved-list">
        <div v-for="item in historyItems" :key="item.id" class="saved-item">
          <button class="saved-load" @click="openSaved(item)">{{ item.title }}</button>
          <button class="saved-del" @click="removeSaved(item.id)" aria-label="삭제">✕</button>
        </div>
      </div>
    </section>

    <section v-if="shouldShowProgress" class="progress-wrap">
      <SearchProgress />
    </section>

    <section v-if="shouldShowResult" class="result-wrap">
      <StreamResult />
    </section>
  </div>
</template>

<script setup lang="ts">
import SearchProgress from '@/components/SearchProgress.vue';
import StreamResult from '@/components/StreamResult.vue';
import { useStreamStore } from '@/stores/stream';
import { useHistoryStore, type SavedPlan } from '@/stores/history';
import { computed, onMounted, onUnmounted, ref } from 'vue';

const streamStore = useStreamStore();
const streamState = streamStore.state;

const historyStore = useHistoryStore();
const historyItems = computed(() => historyStore.items);
// 진입 시 서버 저장소와 동기화(다른 기기에서 저장한 계획도 표시).
onMounted(() => historyStore.refresh());
const openSaved = async (item: SavedPlan) => {
  // 요약만 있는 항목(다른 기기 저장분)은 서버에서 상세를 로드.
  const full = await historyStore.load(item.id);
  const src = full ?? item;
  streamStore.loadSaved(src.plan, src.flights);
  setTimeout(() => {
    document.querySelector('.result-wrap')?.scrollIntoView({ behavior: 'smooth' });
  }, 60);
};
const removeSaved = (id: string) => historyStore.remove(id);

// 자연어 입력 하나로 통일. 콤보/시군구 코드 제거 → 서버(LLM)가 문장에서 조건을 판단.
const planText = ref('');

// 선택형 빠른 추가 — 클릭하면 문장에 자연스럽게 덧붙는다.
const quickGroups = [
  { title: '기간', chips: ['당일치기', '1박 2일', '2박 3일', '3박 4일', '일주일'] },
  { title: '동행', chips: ['혼자', '커플', '가족', '친구', '부모님과'] },
  { title: '스타일', chips: ['맛집 위주', '힐링', '배낭여행', '신혼여행', '도보 여행', '쇼핑'] },
  { title: '예산', chips: ['가성비', '보통', '넉넉하게'] },
];

const appendChip = (chip: string) => {
  const cur = planText.value.trim();
  if (cur.includes(chip)) return;
  planText.value = cur ? `${cur} ${chip}` : chip;
};

const isFormValid = computed(() => planText.value.trim().length > 0);

const shouldShowProgress = computed(() =>
  streamState.isStreaming ||
  streamState.progress.messages.length > 0 ||
  streamState.progress.stage !== 'idle'
);

const shouldShowResult = computed(() =>
  !!streamState.plan || !!streamState.error || streamState.isStreaming ||
  (!!streamState.currentData && streamState.currentData.trim().length > 0)
);

const generateTravelPlan = async () => {
  if (!isFormValid.value || streamState.isStreaming) return;
  await streamStore.startTravelPlanStream(planText.value.trim());
  setTimeout(() => {
    document.querySelector('.progress-wrap')?.scrollIntoView({ behavior: 'smooth' });
  }, 120);
};

onUnmounted(() => streamStore.stopStream());
</script>

<style scoped>
.travel-plan-view {
  min-height: calc(100vh - 70px);
  background: var(--wa-warm);
  padding: clamp(2rem, 4vw, 3.5rem) 1.25rem 4rem;
  color: var(--wa-text-dark);
}

.intro {
  max-width: 900px;
  margin: 0 auto 2rem;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  align-items: center;
}
.eyebrow {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--wa-terra);
}
.title {
  font-family: var(--wa-font-serif);
  font-size: clamp(2rem, 3vw + 1rem, 3.25rem);
  color: var(--wa-ocean);
  font-weight: 500;
  letter-spacing: -0.015em;
  margin: 0;
  line-height: 1.1;
}
.title em { color: var(--wa-terra); font-style: italic; font-weight: 500; }
.subtitle {
  max-width: 640px;
  font-family: var(--wa-font-sans);
  font-size: 1rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  margin: 0.5rem 0 0;
}

/* ---- Form card ---- */
.plan-card {
  max-width: 960px;
  margin: 0 auto;
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 28px;
  padding: clamp(1.5rem, 3vw, 2.5rem);
  box-shadow: 0 40px 80px -50px color-mix(in srgb, var(--wa-ocean) 55%, transparent);
}

.plan-form { display: flex; flex-direction: column; gap: 1.5rem; }

.form-section {
  background: var(--wa-warm);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 45%, transparent);
  border-radius: 22px;
  padding: 1.5rem;
}
.section-head {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  margin-bottom: 1rem;
}
.section-bar {
  width: 4px;
  height: 20px;
  background: var(--wa-ocean);
  border-radius: 2px;
}
.section-head h3 {
  margin: 0;
  font-family: var(--wa-font-serif);
  font-size: 1.25rem;
  color: var(--wa-ocean);
  font-weight: 500;
  letter-spacing: -0.005em;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin-bottom: 0.75rem;
}
.form-row--single { grid-template-columns: 1fr; }
.form-row:last-child { margin-bottom: 0; }

.form-group { display: flex; flex-direction: column; gap: 0.375rem; }
.form-group-wide { grid-column: 1 / -1; }
.form-group > span {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--wa-text-mid);
}

.form-select, .form-input {
  padding: 0.75rem 1rem;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 12px;
  background: var(--wa-cream);
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-dark);
  outline: none;
  transition: border-color 150ms ease, box-shadow 150ms ease;
}
.form-select:hover, .form-input:hover { border-color: color-mix(in srgb, var(--wa-ocean) 35%, transparent); }
.form-select:focus, .form-input:focus {
  border-color: var(--wa-ocean);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--wa-ocean) 14%, transparent);
}
.form-select:disabled { opacity: 0.6; cursor: not-allowed; }

.chip-group { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.chip-option {
  display: inline-flex;
  align-items: center;
  padding: 0.375rem 0.875rem;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--wa-ocean) 25%, transparent);
  background: transparent;
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-text-dark);
  cursor: pointer;
  user-select: none;
  transition: all 150ms ease;
}
.chip-option input { display: none; }
.chip-option:hover { background: color-mix(in srgb, var(--wa-ocean) 8%, transparent); }
.chip-option:has(input:checked) {
  background: var(--wa-ocean);
  border-color: var(--wa-ocean);
  color: var(--wa-cream);
}

.form-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding-top: 0.5rem;
}
.submit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  min-width: 220px;
  padding: 0 2.25rem;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--wa-ocean), var(--wa-dusk));
  color: var(--wa-cream);
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  font-weight: 500;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease;
  box-shadow: 0 18px 36px -16px color-mix(in srgb, var(--wa-ocean) 65%, transparent);
}
.submit-button:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 24px 48px -20px color-mix(in srgb, var(--wa-ocean) 75%, transparent); }
.submit-button:disabled { opacity: 0.45; cursor: not-allowed; }
.spinner {
  width: 18px; height: 18px;
  border: 2px solid color-mix(in srgb, var(--wa-cream) 30%, transparent);
  border-top-color: var(--wa-cream);
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}
.hint {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-light);
  margin: 0;
  letter-spacing: 0.04em;
}

.progress-wrap, .result-wrap {
  max-width: 960px;
  margin: 2rem auto 0;
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

@media (max-width: 720px) {
  .form-row { grid-template-columns: 1fr; }
  .plan-card { padding: 1.25rem; border-radius: 22px; }
  .form-section { padding: 1.25rem; }
}
</style>

<style scoped>
/* --- 자연어 입력 폼 (2026-07 재구성) --- */
.nl-label {
  display: block;
  font-family: var(--wa-font-serif);
  font-size: 1.15rem;
  color: var(--wa-ocean);
  margin-bottom: 0.75rem;
  font-weight: 600;
}
.nl-input {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  min-height: 120px;
  padding: 1rem 1.15rem;
  font-family: var(--wa-font-sans);
  font-size: 1rem;
  line-height: 1.6;
  color: var(--wa-text-dark);
  background: #fff;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 16px;
  outline: none;
  transition: border-color 160ms ease, box-shadow 160ms ease;
}
.nl-input:focus {
  border-color: var(--wa-ocean);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--wa-ocean) 14%, transparent);
}
.nl-input:disabled { opacity: 0.6; }

.quick-add { margin-top: 1.25rem; }
.qa-label {
  font-size: 0.72rem; letter-spacing: 0.12em; text-transform: uppercase;
  color: var(--wa-text-light); font-weight: 600;
}
.qa-groups { display: flex; flex-direction: column; gap: 0.65rem; margin-top: 0.65rem; }
.qa-group { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
.qa-group-title { font-size: 0.78rem; color: var(--wa-text-mid); min-width: 44px; }
.qa-chips { display: flex; flex-wrap: wrap; gap: 0.4rem; }
.qa-chip {
  background: color-mix(in srgb, var(--wa-sand) 30%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-sand) 60%, transparent);
  border-radius: 999px;
  padding: 0.3rem 0.8rem;
  font-size: 0.82rem;
  color: var(--wa-text-dark);
  cursor: pointer;
  transition: all 140ms ease;
}
.qa-chip:hover:not(:disabled) { background: var(--wa-ocean); color: var(--wa-cream); border-color: var(--wa-ocean); }
.qa-chip:disabled { opacity: 0.45; cursor: not-allowed; }
</style>

<style scoped>
/* --- 저장한 여행 --- */
.saved-strip { max-width: 960px; margin: 1.5rem auto 0; }
.saved-title { font-size: 0.9rem; font-weight: 700; color: var(--wa-ocean); margin: 0 0 0.6rem; }
.saved-list { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.saved-item {
  display: inline-flex; align-items: center;
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 60%, transparent);
  border-radius: 999px; overflow: hidden;
}
.saved-load {
  background: transparent; border: none; cursor: pointer;
  padding: 0.4rem 0.5rem 0.4rem 0.9rem; font-size: 0.85rem; color: var(--wa-text-dark);
}
.saved-load:hover { color: var(--wa-terra); }
.saved-del {
  background: transparent; border: none; cursor: pointer;
  padding: 0.4rem 0.7rem 0.4rem 0.4rem; font-size: 0.75rem; color: var(--wa-text-light);
}
.saved-del:hover { color: #b14a4a; }
</style>
