<script setup lang="ts">
import SearchProgress from '@/components/SearchProgress.vue';
import StreamResult from '@/components/StreamResult.vue';
import { useStreamStore } from '@/stores/stream';
import { computed, onUnmounted, ref } from 'vue';

const streamStore = useStreamStore();
const streamState = streamStore.state;
const searchQuery = ref('');
const isSearching = computed(() => streamState.isStreaming);

const shouldShowProgress = computed(() =>
  streamState.isStreaming ||
  streamState.progress.messages.length > 0 ||
  streamState.progress.stage !== 'idle'
);

const shouldShowResult = computed(() => {
  const hasData = !!streamState.plan ||
    (streamState.currentData && streamState.currentData.trim().length > 0);
  return hasData || !!streamState.error;
});

const presets = [
  '부산 해운대에서 2박 3일, 해산물과 야경 위주',
  '제주 서귀포 가족 여행 3박 4일, 아이 있는 코스',
  '강릉 커피 투어 당일치기',
  '전주 한옥마을 + 맛집 위주 1박 2일',
];

const usePreset = (preset: string) => {
  searchQuery.value = preset;
};

const handleSearch = async () => {
  if (!searchQuery.value.trim() || streamState.isStreaming) return;

  await streamStore.startTravelPlanStream(searchQuery.value.trim());

  setTimeout(() => {
    document.querySelector('.result-section')?.scrollIntoView({ behavior: 'smooth' });
  }, 120);
};

onUnmounted(() => {
  streamStore.stopStream();
});
</script>

<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-bg" aria-hidden="true"></div>

      <div class="hero-content">
        <div class="hero-text">
          <span class="eyebrow">wayn · AI 여행 어시스턴트</span>
          <h1 class="hero-title">
            여행의 처음부터 끝까지,<br />
            <em>함께</em> 만들어가요
          </h1>
          <p class="hero-lead">
            관광공사 공공데이터, 네이버 리뷰, 그리고 Gemini 3 핫스왑 모델이
            실시간으로 움직이는 여행 파이프라인. 한 줄 질의로 완성되는 맞춤 계획.
          </p>

          <form class="hero-search" @submit.prevent="handleSearch">
            <input
              v-model="searchQuery"
              type="text"
              class="hero-input"
              placeholder="예: 부산 해운대 2박 3일, 해산물 맛집과 야경"
              :disabled="isSearching"
            />
            <button type="submit" class="hero-submit" :disabled="isSearching || !searchQuery.trim()">
              <span v-if="isSearching" class="spinner"></span>
              <span v-else>계획 생성</span>
            </button>
          </form>

          <div class="preset-row">
            <span class="preset-label">추천 질의</span>
            <button
              v-for="preset in presets"
              :key="preset"
              class="preset-chip"
              type="button"
              :disabled="isSearching"
              @click="usePreset(preset)"
            >
              {{ preset }}
            </button>
          </div>

          <div class="hero-stats">
            <div class="stat">
              <span class="stat-value">3</span>
              <span class="stat-label">Gemini 3 핫스왑 레이어</span>
            </div>
            <div class="stat-divider" aria-hidden="true"></div>
            <div class="stat">
              <span class="stat-value">실시간</span>
              <span class="stat-label">SSE 진행 이벤트</span>
            </div>
            <div class="stat-divider" aria-hidden="true"></div>
            <div class="stat">
              <span class="stat-value">2 RAG</span>
              <span class="stat-label">관광공사 + 네이버</span>
            </div>
          </div>
        </div>

        <aside class="hero-mock" aria-hidden="true">
          <div class="mock-phone mock-phone--main">
            <div class="mock-notch"></div>
            <div class="mock-screen">
              <div class="mock-header">
                <span class="mock-chip">실시간</span>
                <span class="mock-time">09:24</span>
              </div>
              <div class="mock-title">
                <span>오늘의 일정</span>
                <strong>해운대 해변길</strong>
              </div>
              <ul class="mock-list">
                <li><span class="mock-dot dot-amber"></span><b>09:30</b> 해운대 해수욕장</li>
                <li><span class="mock-dot dot-sage"></span><b>12:00</b> 민락 회센터</li>
                <li><span class="mock-dot dot-terra"></span><b>15:00</b> 해리단길 카페</li>
                <li><span class="mock-dot dot-ocean"></span><b>19:00</b> 광안리 드론쇼</li>
              </ul>
            </div>
          </div>
          <div class="mock-phone mock-phone--side">
            <div class="mock-notch"></div>
            <div class="mock-screen mock-screen--compact">
              <span class="mock-tag">AI 추천</span>
              <p class="mock-quote">
                "해운대의 아침 산책은 동백섬 → 달맞이고개로 이어져야 진짜예요."
              </p>
              <div class="mock-model">gemini-3.1-pro-preview</div>
            </div>
          </div>
        </aside>
      </div>
    </section>

    <!-- Result / Progress -->
    <section class="result-section" v-if="shouldShowProgress || shouldShowResult">
      <SearchProgress v-if="shouldShowProgress" />
      <StreamResult v-if="shouldShowResult" />
    </section>
  </div>
</template>

<style scoped>
.home {
  min-height: calc(100vh - 70px);
  color: var(--wa-text-dark);
  background: var(--wa-warm);
}

/* ============== HERO ============== */
.hero {
  position: relative;
  overflow: hidden;
  padding: clamp(3rem, 6vw, 5.5rem) 1.5rem clamp(4rem, 6vw, 6rem);
}
.hero-bg {
  position: absolute;
  inset: -10% -20% -20% -20%;
  background:
    radial-gradient(circle at 18% 20%, color-mix(in srgb, var(--wa-amber) 28%, transparent), transparent 45%),
    radial-gradient(circle at 85% 10%, color-mix(in srgb, var(--wa-terra) 22%, transparent), transparent 50%),
    radial-gradient(circle at 60% 80%, color-mix(in srgb, var(--wa-sage) 24%, transparent), transparent 55%),
    linear-gradient(180deg, var(--wa-cream), var(--wa-warm) 80%);
  pointer-events: none;
  z-index: 0;
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: clamp(2rem, 4vw, 4rem);
  align-items: center;
}

.hero-text { display: flex; flex-direction: column; }

.eyebrow {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--wa-terra);
  margin-bottom: 1rem;
}

.hero-title {
  font-family: var(--wa-font-serif);
  font-size: clamp(2.5rem, 3vw + 1.5rem, 4rem);
  font-weight: 500;
  line-height: 1.08;
  color: var(--wa-ocean);
  letter-spacing: -0.015em;
  margin: 0 0 1.25rem;
}
.hero-title em {
  color: var(--wa-terra);
  font-style: italic;
  font-weight: 500;
}

.hero-lead {
  font-family: var(--wa-font-sans);
  font-size: 1.0625rem;
  line-height: 1.6;
  color: var(--wa-text-mid);
  margin: 0 0 2rem;
  max-width: 560px;
}

/* Search pill */
.hero-search {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: #ffffff;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 65%, transparent);
  border-radius: 999px;
  padding: 0.375rem 0.375rem 0.375rem 1.5rem;
  box-shadow: 0 20px 40px -24px color-mix(in srgb, var(--wa-ocean) 35%, transparent);
  transition: box-shadow 180ms ease, border-color 180ms ease;
  max-width: 640px;
}
.hero-search:focus-within {
  border-color: var(--wa-ocean);
  box-shadow: 0 28px 48px -24px color-mix(in srgb, var(--wa-ocean) 45%, transparent);
}
.hero-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-family: var(--wa-font-sans);
  font-size: 1rem;
  color: var(--wa-text-dark);
  padding: 0.875rem 0;
  box-shadow: none !important;
}
.hero-input::placeholder { color: var(--wa-text-light); }
.hero-input:focus { box-shadow: none; }
.hero-input:disabled { opacity: 0.6; }

.hero-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  min-width: 132px;
  padding: 0 1.5rem;
  border-radius: 999px;
  background: var(--wa-ocean);
  color: var(--wa-cream);
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  font-weight: 500;
  letter-spacing: 0.02em;
  border: none;
  cursor: pointer;
  transition: background 180ms ease, transform 180ms ease;
}
.hero-submit:hover:not(:disabled) { background: var(--wa-dusk); transform: translateY(-1px); }
.hero-submit:disabled { opacity: 0.55; cursor: not-allowed; }

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid color-mix(in srgb, var(--wa-cream) 30%, transparent);
  border-top-color: var(--wa-cream);
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}

.preset-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 1.5rem;
}
.preset-label {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--wa-text-light);
  margin-right: 0.25rem;
}
.preset-chip {
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--wa-ocean) 25%, transparent);
  border-radius: 999px;
  padding: 0.375rem 0.875rem;
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-ocean);
  cursor: pointer;
  transition: all 150ms ease;
}
.preset-chip:hover:not(:disabled) {
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-color: var(--wa-ocean);
}
.preset-chip:disabled { opacity: 0.4; cursor: not-allowed; }

.hero-stats {
  display: flex;
  align-items: stretch;
  gap: 1.5rem;
  margin-top: 2.25rem;
  padding-top: 1.75rem;
  border-top: 1px solid color-mix(in srgb, var(--wa-sand) 60%, transparent);
}
.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-value {
  font-family: var(--wa-font-serif);
  font-size: 1.5rem;
  font-style: italic;
  color: var(--wa-terra);
  font-weight: 500;
}
.stat-label {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-mid);
  letter-spacing: 0.03em;
}
.stat-divider {
  width: 1px;
  background: color-mix(in srgb, var(--wa-sand) 70%, transparent);
}

/* Phone mocks */
.hero-mock {
  position: relative;
  height: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mock-phone {
  position: absolute;
  width: 240px;
  border-radius: 38px;
  padding: 10px;
  background: var(--wa-ocean);
  box-shadow: 0 30px 60px -20px color-mix(in srgb, var(--wa-ocean) 55%, transparent);
}
.mock-phone--main {
  top: 10px;
  left: 50%;
  transform: translateX(-55%) rotate(-4deg);
  height: 420px;
}
.mock-phone--side {
  top: 130px;
  right: 0;
  transform: rotate(6deg);
  width: 220px;
  height: 260px;
  background: var(--wa-terra);
}
.mock-notch {
  width: 60px;
  height: 5px;
  background: color-mix(in srgb, var(--wa-cream) 40%, transparent);
  border-radius: 999px;
  margin: 4px auto 8px;
}
.mock-screen {
  background: var(--wa-cream);
  border-radius: 28px;
  padding: 1.25rem 1.25rem 1.5rem;
  height: calc(100% - 22px);
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.mock-screen--compact {
  background: var(--wa-warm);
  padding: 1rem;
  gap: 0.5rem;
}

.mock-header { display: flex; justify-content: space-between; align-items: center; }
.mock-chip {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--wa-terra);
}
.mock-time {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-light);
}
.mock-title {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.mock-title span {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-light);
  letter-spacing: 0.04em;
}
.mock-title strong {
  font-family: var(--wa-font-serif);
  font-size: 1.25rem;
  color: var(--wa-ocean);
  font-style: italic;
  font-weight: 500;
}
.mock-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}
.mock-list li {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-text-dark);
}
.mock-list li b { color: var(--wa-terra); margin-right: 4px; font-weight: 600; }
.mock-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--wa-sage);
}
.dot-amber { background: var(--wa-amber); }
.dot-sage  { background: var(--wa-sage); }
.dot-terra { background: var(--wa-terra); }
.dot-ocean { background: var(--wa-ocean); }

.mock-tag {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--wa-cream);
  background: var(--wa-terra);
  padding: 3px 10px;
  border-radius: 999px;
  align-self: flex-start;
}
.mock-quote {
  font-family: var(--wa-font-serif);
  font-size: 1rem;
  color: var(--wa-ocean);
  font-style: italic;
  line-height: 1.4;
  margin: 0;
}
.mock-model {
  margin-top: auto;
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.6875rem;
  color: var(--wa-text-light);
  letter-spacing: 0.04em;
}

/* Result */
.result-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1.5rem 4rem;
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

@media (max-width: 960px) {
  .hero-content { grid-template-columns: 1fr; }
  .hero-mock { height: 360px; margin-top: 1rem; }
  .mock-phone--main { width: 200px; height: 340px; }
  .mock-phone--side { width: 170px; height: 220px; top: 120px; }
}

@media (max-width: 640px) {
  .hero { padding: 2.5rem 1.125rem 3rem; }
  .hero-search { flex-direction: column; align-items: stretch; border-radius: 24px; padding: 0.5rem; }
  .hero-input { padding: 0.75rem 1rem; }
  .hero-submit { width: 100%; }
  .hero-mock { display: none; }
  .hero-stats { flex-wrap: wrap; gap: 1rem; }
  .stat-divider { display: none; }
}
</style>
