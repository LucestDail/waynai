<template>
  <div class="stream-result">
    <!-- Error -->
    <div v-if="streamState.error" class="error-container">
      <div class="error-icon">!</div>
      <div>
        <h3 class="error-title">오류가 발생했습니다</h3>
        <p class="error-message">{{ streamState.error }}</p>
      </div>
      <button @click="handleRetry" class="retry-button">다시 시도</button>
    </div>

    <!-- Structured plan -->
    <div v-else-if="plan" class="result-card">
      <div class="browser-bar" aria-hidden="true">
        <span class="dot dot-r"></span>
        <span class="dot dot-y"></span>
        <span class="dot dot-g"></span>
        <div class="url-pill">waynai.app / plan / {{ plan.destination || 'trip' }}</div>
      </div>

      <header class="plan-header">
        <div class="plan-meta">
          <span class="eyebrow">AI 여행 계획</span>
          <h2 class="plan-title">{{ plan.destination || plan.theme || '맞춤 여행' }}</h2>
          <p class="plan-sub" v-if="plan.summary">{{ plan.summary }}</p>
        </div>
        <div class="plan-chips">
          <span v-if="plan.duration" class="chip">{{ plan.duration }}</span>
          <span v-if="plan.theme" class="chip">{{ plan.theme }}</span>
          <span v-if="plan.budget" class="chip">예산 · {{ plan.budget }}</span>
        </div>
      </header>

      <!-- 항공권 -->
      <section v-if="flights.length" class="flights-block">
        <h4 class="section-title">✈️ 항공권 <span class="muted">최저가 · Travelpayouts</span></h4>
        <ul class="flight-list">
          <li v-for="(f, i) in flights" :key="i" class="flight-card">
            <div class="flight-route">
              <strong>{{ f.origin }} → {{ f.destination }}</strong>
              <span class="flight-badge trip">{{ f.roundTrip ? '왕복' : '편도' }}</span>
              <span v-if="f.transfers != null" class="flight-badge" :class="{ direct: f.transfers === 0 }">
                {{ f.transfers === 0 ? '직항' : f.transfers + '회 경유' }}
              </span>
            </div>
            <div class="flight-meta">
              <span v-if="f.airline" class="flight-airline">{{ f.airline }}</span>
              <span v-if="f.legMinutes" class="flight-dur">편도 {{ fmtDuration(f.legMinutes) }}</span>
              <span v-if="f.departureAt" class="flight-date">출발 {{ f.departureAt.slice(0, 10) }}</span>
              <span v-if="f.roundTrip && f.returnAt" class="flight-date">귀국 {{ f.returnAt.slice(0, 10) }}</span>
            </div>
            <div class="flight-foot">
              <span class="flight-price">{{ formatPrice(f.price, f.currency) }}<span class="fp-unit"> · {{ f.roundTrip ? '왕복' : '편도' }}</span></span>
              <a v-if="f.bookingUrl" :href="f.bookingUrl" target="_blank" rel="noopener" class="flight-book">예약하기</a>
            </div>
          </li>
        </ul>
      </section>

      <!-- 총 예상 비용 -->
      <div v-if="totalEstimate > 0" class="cost-summary">
        <span class="cost-label">총 예상 비용</span>
        <span class="cost-value">{{ formatPrice(totalEstimate, 'KRW') }}</span>
        <span class="cost-note" v-if="plan.estimatedBudgetKrw && flights.length">현지 경비 + 항공권 포함</span>
      </div>

      <div class="plan-actions">
        <button @click="savePlan" class="action-button" :disabled="isSaved">
          <span>{{ isSaved ? '저장됨 ✓' : '내 여행에 저장' }}</span>
        </button>
        <button @click="copyToClipboard" class="action-button" :disabled="isCopying">
          <span v-if="isCopying" class="spinner-mini"></span>
          <span v-else>클립보드 복사</span>
        </button>
        <button @click="downloadPDF" class="action-button primary" :disabled="isGeneratingPDF">
          <span v-if="isGeneratingPDF" class="spinner-mini"></span>
          <span v-else>PDF 다운로드</span>
        </button>
      </div>

      <!-- 숙소 (실데이터) -->
      <section v-if="plan.accommodation && plan.accommodation.name" class="stay-block">
        <h4 class="section-title">🏨 추천 숙소</h4>
        <div class="stay-card">
          <div class="stay-main">
            <strong class="stay-name">{{ plan.accommodation.name }}</strong>
            <span class="stay-meta">
              <span v-if="plan.accommodation.type">{{ plan.accommodation.type }}</span>
              <span v-if="plan.accommodation.area">· {{ plan.accommodation.area }}</span>
            </span>
          </div>
          <div class="stay-foot">
            <span v-if="plan.accommodation.pricePerNightKrw" class="stay-price">1박 {{ plan.accommodation.pricePerNightKrw.toLocaleString('ko-KR') }}원~</span>
            <a v-if="plan.accommodation.bookingUrl" :href="plan.accommodation.bookingUrl" target="_blank" rel="noopener" class="stay-book">숙소 예약</a>
          </div>
        </div>
      </section>

      <!-- 여행 개요: 날씨 · 현지 상황 · 준비물 · 비용 -->
      <section v-if="hasTripInfo" class="trip-info">
        <div v-if="plan.weatherInfo" class="ti-card">
          <h4>☀︎ 날씨·기후</h4><p>{{ plan.weatherInfo }}</p>
        </div>
        <div v-if="plan.localInfo" class="ti-card">
          <h4>📍 현지 상황</h4><p>{{ plan.localInfo }}</p>
        </div>
        <div v-if="plan.packingList && plan.packingList.length" class="ti-card">
          <h4>🎒 준비물</h4>
          <ul class="ti-pack"><li v-for="(p, i) in plan.packingList" :key="i">{{ p }}</li></ul>
        </div>
        <div v-if="costRows.length" class="ti-card">
          <h4>💰 예상 비용</h4>
          <ul class="ti-cost">
            <li v-for="r in costRows" :key="r.k"><span>{{ r.k }}</span><b>{{ r.v }}</b></li>
          </ul>
        </div>
      </section>

      <!-- 일자별 책자 (좌: 일정/정보 · 우: 지도/경로/주요위치) -->
      <section class="booklet" v-if="plan.itinerary && plan.itinerary.length">
        <article v-for="(day, idx) in plan.itinerary" :key="idx" class="spread">
          <div class="spread-left">
            <div class="spread-daybadge">Day {{ day.day ?? idx + 1 }}</div>
            <h3 class="spread-title">{{ day.title || `${day.day ?? idx + 1}일차` }}</h3>
            <p v-if="day.overview" class="spread-overview">{{ day.overview }}</p>

            <div class="spread-facts">
              <div v-if="day.weather" class="fact"><span class="fact-ic">☀︎</span><div><b>예상 날씨</b><span>{{ day.weather }}</span></div></div>
              <div v-if="day.accommodation" class="fact"><span class="fact-ic">🏨</span><div><b>숙소</b><span>{{ day.accommodation }}</span></div></div>
              <div v-if="day.transportation" class="fact"><span class="fact-ic">🚌</span><div><b>이동</b><span>{{ day.transportation }}</span></div></div>
            </div>

            <!-- 타임테이블: 방문지(시간순) -->
            <div class="tt-label" v-if="day.spots && day.spots.length">🕘 하루 타임테이블</div>
            <ol class="timetable" v-if="day.spots && day.spots.length">
              <li v-for="(s, i) in day.spots" :key="i" class="tt-row">
                <span class="tt-time">{{ s.visitTime || '—' }}<em v-if="s.durationMin">{{ s.durationMin }}분</em></span>
                <div class="tt-body">
                  <strong>{{ s.name }}</strong>
                  <span v-if="s.activity" class="tt-act">{{ s.activity }}</span>
                  <span v-if="s.notes" class="tt-note">{{ s.notes }}</span>
                </div>
              </li>
            </ol>
            <ul class="bullet" v-else-if="day.activities && day.activities.length">
              <li v-for="(act, i) in day.activities" :key="i">{{ act }}</li>
            </ul>

            <!-- 식당 (구조화) -->
            <div class="meals-block" v-if="day.meals && day.meals.length">
              <div class="mb-label">🍽️ 식사</div>
              <div v-for="(m, i) in day.meals" :key="i" class="meal-row">
                <span class="meal-type">{{ m.type || '식사' }}</span>
                <div class="meal-body">
                  <strong>{{ m.name }}</strong>
                  <span v-if="m.menu" class="meal-menu">{{ m.menu }}</span>
                  <span class="meal-sub">
                    <span v-if="m.location">📍 {{ m.location }}</span>
                    <span v-if="m.priceKrw" class="meal-price">{{ m.priceKrw.toLocaleString('ko-KR') }}원</span>
                  </span>
                </div>
              </div>
            </div>

            <!-- 일자 비용 -->
            <div class="daycost" v-if="(day.costItems && day.costItems.length) || day.estimatedCost">
              <span class="dc-label">💰 예상 비용</span>
              <ul v-if="day.costItems && day.costItems.length" class="dc-items">
                <li v-for="(c, i) in day.costItems" :key="i"><span>{{ c.label }}</span><b>{{ (c.krw || 0).toLocaleString('ko-KR') }}원</b></li>
              </ul>
              <span v-else class="dc-str">{{ day.estimatedCost }}</span>
            </div>

            <p v-if="day.tips" class="spread-tip">💡 {{ day.tips }}</p>
          </div>

          <div class="spread-right">
            <TravelMap v-if="dayHasCoords(day)" :days="[day]" :profile="mapProfile" />
            <div v-else class="spread-nomap">이 날의 지도 좌표가 아직 없어요</div>
            <div class="spread-places" v-if="day.spots && day.spots.length">
              <span class="sp-title">주요 위치 · 동선</span>
              <ol>
                <li v-for="(s, i) in day.spots" :key="i"><span class="sp-idx">{{ i + 1 }}</span>{{ s.name }}</li>
              </ol>
            </div>
          </div>
        </article>
      </section>

      <section v-if="plan.tips && plan.tips.length" class="tips-block">
        <h4 class="tips-title">여행 팁</h4>
        <ul class="bullet">
          <li v-for="(tip, i) in plan.tips" :key="i">{{ tip }}</li>
        </ul>
      </section>
    </div>

    <!-- 생성 중: 실시간으로 일정이 쌓이는 미리보기 (JSON 대신 사람이 읽는 카드) -->
    <div v-else-if="isBuilding" class="result-card building-card">
      <header class="plan-header build-header">
        <div class="plan-meta">
          <span class="eyebrow">AI가 일정을 만드는 중</span>
          <h2 class="plan-title">
            {{ partial?.destination || '여행 일정을 구성하고 있어요' }}<span class="build-cursor">▋</span>
          </h2>
          <p class="plan-sub" v-if="partial?.summary">{{ partial.summary }}</p>
          <p class="plan-sub build-count" v-else>
            지금까지 {{ buildingDays.length }}일 · 볼거리 {{ buildingSpotCount }}곳 구성
          </p>
        </div>
      </header>

      <div v-if="buildingDays.length" class="build-days">
        <div v-for="(day, idx) in buildingDays" :key="idx" class="build-day">
          <div class="build-day-head">Day {{ day.day ?? idx + 1 }}<span v-if="day.title"> · {{ day.title }}</span></div>
          <ul class="build-spots" v-if="day.spots && day.spots.length">
            <li v-for="(s, i) in day.spots" :key="i">
              <span class="bs-time" v-if="s.visitTime">{{ s.visitTime }}</span>
              <span class="bs-name">{{ s.name }}</span>
            </li>
          </ul>
        </div>
        <div class="build-more"><span class="dot-flash"></span> 다음 일정을 구성하는 중…</div>
      </div>
      <div v-else class="build-warming">
        <span class="build-spinner"></span>
        여행 정보를 모아 일정을 짜고 있어요… 잠시만요.
      </div>
    </div>

    <!-- 완료됐지만 구조화 실패 (raw JSON 노출 금지 → 친근한 안내) -->
    <div v-else-if="showTextResult && !looksLikeJson" class="result-card bubble-card">
      <div class="ai-bubble">
        <div class="ai-avatar">AI</div>
        <div class="stream-text" v-html="formattedData"></div>
      </div>
    </div>
    <div v-else-if="showTextResult && looksLikeJson" class="result-card soft-fail">
      <h3 class="sf-title">일정을 정리하지 못했어요</h3>
      <p class="sf-desc">요청을 조금 더 구체적으로 입력하고 다시 시도해 주세요. (예: “오사카 3박4일 커플 맛집 여행”)</p>
      <button @click="handleRetry" class="action-button primary">다시 시도</button>
    </div>

    <!-- Empty -->
    <div v-else class="empty-state">
      <span class="empty-tag">준비 완료</span>
      <h4 class="empty-title">여행 계획을 만들어 드릴게요</h4>
      <p class="empty-desc">가고 싶은 곳과 일정을 한 문장으로 적어주시면, 항공권부터 동선·비용까지 맞춰 드려요.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useStreamStore } from '@/stores/stream';
import { useHistoryStore } from '@/stores/history';
import TravelMap from '@/components/TravelMap.vue';
// html2canvas-pro: 최신 CSS 색상함수(color-mix/oklch) 지원. 기존 html2canvas 1.x 는 color-mix 파싱 실패로 PDF 오류.
import html2canvas from 'html2canvas-pro';
import jsPDF from 'jspdf';

declare module 'jspdf' {
  interface jsPDF {
    addImage(imageData: string, format: string, x: number, y: number, width: number, height: number): void;
    addPage(): void;
    save(filename: string): void;
  }
}

const streamStore = useStreamStore();
const streamState = streamStore.state;
const { formatMarkdown } = streamStore;

const isCopying = ref(false);
const isGeneratingPDF = ref(false);

const plan = computed(() => streamState.plan);
const hasText = computed(() => !!streamState.currentData && streamState.currentData.trim().length > 0);

const historyStore = useHistoryStore();
const isSaved = ref(false);
// 새 계획이 오면 저장 상태 초기화.
watch(() => streamState.plan, () => { isSaved.value = false; });
const savePlan = () => {
  if (!streamState.plan || isSaved.value) return;
  historyStore.save(streamState.plan, flights.value);
  isSaved.value = true;
  showToast('내 여행에 저장했어요.');
};

const formattedData = computed(() => {
  if (!streamState.currentData) return '';
  return formatMarkdown(streamState.currentData);
});

// 항공권: plan 에 부착된 것이 우선, 없으면 sources.flight 이벤트로 받은 것.
const flights = computed(() => {
  const fromPlan = streamState.plan?.flights;
  if (fromPlan && fromPlan.length) return fromPlan;
  return streamState.flights || [];
});

// 지도 경로 프로파일: 도보/산책 스타일이면 보행자, 그 외 차량.
const mapProfile = computed(() => {
  const style = streamState.progress.intent?.style || '';
  return /도보|산책|walk/i.test(style) ? 'foot-walking' : 'driving-car';
});

// 날짜별 좌표 유무 (책자 우측 지도 표시 여부).
type Day = NonNullable<NonNullable<typeof streamState.plan>['itinerary']>[number];
const dayHasCoords = (day: Day) =>
  (day.spots || []).some((s) => typeof s.latitude === 'number' && typeof s.longitude === 'number');

// 여행 개요 섹션 표시 여부.
const hasTripInfo = computed(() => {
  const p = streamState.plan;
  if (!p) return false;
  return !!(p.weatherInfo || p.localInfo || (p.packingList && p.packingList.length) || costRows.value.length);
});

// 비용 분해 → 화면용 행.
const costRows = computed(() => {
  const c = streamState.plan?.costBreakdown;
  if (!c) return [] as { k: string; v: string }[];
  const won = (n?: number) => (typeof n === 'number' && n > 0 ? `${n.toLocaleString('ko-KR')}원` : '');
  const rows = [
    { k: '항공', v: won(c.flightsKrw) },
    { k: '숙박', v: won(c.accommodationKrw) },
    { k: '식비', v: won(c.foodKrw) },
    { k: '교통', v: won(c.transportKrw) },
    { k: '입장/액티비티', v: won(c.activitiesKrw) },
    { k: '기타', v: won(c.etcKrw) },
  ];
  return rows.filter((r) => r.v);
});

// 생성 중 실시간 빌드업.
const isBuilding = computed(() => streamState.isStreaming && !streamState.plan && !streamState.error);
const partial = computed(() => streamState.partialPlan);
const buildingDays = computed(() => partial.value?.itinerary || []);
const buildingSpotCount = computed(() =>
  buildingDays.value.reduce((n, d) => n + ((d.spots && d.spots.length) || 0), 0),
);

// 스트리밍이 끝났는데 구조화 plan 이 없을 때만 텍스트 결과/안내 노출 (진행 중엔 숨김).
const showTextResult = computed(() =>
  !streamState.isStreaming && !streamState.plan && !streamState.error && hasText.value,
);
// currentData 가 JSON 같으면(개발자 출력) 화면에 그대로 노출하지 않는다.
const looksLikeJson = computed(() => {
  const t = streamState.currentData.trim();
  return t.startsWith('{') || t.startsWith('```') || t.startsWith('[');
});

const formatPrice = (v?: number, currency?: string) => {
  if (typeof v !== 'number') return '';
  const cur = (currency || 'KRW').toUpperCase();
  return `${v.toLocaleString('ko-KR')} ${cur}`;
};

const fmtDuration = (min?: number) => {
  if (!min || min <= 0) return '';
  const h = Math.floor(min / 60);
  const m = min % 60;
  return h ? `${h}시간${m ? ' ' + m + '분' : ''}` : `${m}분`;
};

// 총 예상 비용: 계획 예산 + 최저 항공권.
const totalEstimate = computed(() => {
  const base = streamState.plan?.estimatedBudgetKrw || 0;
  const cheapest = flights.value.reduce((min, f) => {
    const p = f.price ?? Infinity;
    return p < min ? p : min;
  }, Infinity);
  const air = Number.isFinite(cheapest) ? cheapest : 0;
  return base + air;
});

const handleRetry = () => {
  window.location.reload();
};

const copyToClipboard = async () => {
  const source = plan.value ? JSON.stringify(plan.value, null, 2) : streamState.currentData;
  if (!source) return;
  isCopying.value = true;
  try {
    const text = plan.value ? source : stripHtmlTags(source);
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      await fallbackCopyTextToClipboard(text);
    }
    showToast('여행 계획이 클립보드에 복사되었습니다.');
  } catch (error) {
    console.error('복사 실패:', error);
    showToast('복사에 실패했습니다. 다시 시도해주세요.');
  } finally {
    isCopying.value = false;
  }
};

const downloadPDF = async () => {
  isGeneratingPDF.value = true;
  try {
    const element = (document.querySelector('.result-card') || document.querySelector('.timeline') || document.querySelector('.stream-text')) as HTMLElement | null;
    if (!element) throw new Error('PDF 생성할 요소가 없습니다.');
    // 1차: 지도 포함 시도(crossOrigin 타일). 실패(캔버스 오염 등) 시 2차로 지도 제외 재시도.
    let canvas;
    try {
      canvas = await html2canvas(element, { scale: 2, useCORS: true, backgroundColor: '#f5f0e8' });
    } catch (mapErr) {
      console.warn('지도 포함 PDF 실패 → 지도 제외 재시도:', mapErr);
      canvas = await html2canvas(element, {
        scale: 2, useCORS: true, backgroundColor: '#f5f0e8',
        ignoreElements: (el) => (el as HTMLElement).classList?.contains('travel-map'),
      });
    }
    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const imgWidth = 210;
    const pageHeight = 295;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    let heightLeft = imgHeight;
    let position = 0;
    pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
    heightLeft -= pageHeight;
    while (heightLeft >= 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }
    const fileName = `waynai_plan_${new Date().toISOString().split('T')[0]}.pdf`;
    pdf.save(fileName);
    showToast('PDF가 다운로드되었습니다.');
  } catch (error) {
    console.error('PDF 생성 실패:', error);
    showToast('PDF 생성에 실패했습니다.');
  } finally {
    isGeneratingPDF.value = false;
  }
};

const stripHtmlTags = (html: string): string => {
  const temp = document.createElement('div');
  temp.innerHTML = html;
  return temp.textContent || temp.innerText || '';
};

const fallbackCopyTextToClipboard = (text: string): Promise<void> => {
  return new Promise((resolve, reject) => {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.cssText = 'position:fixed;top:0;left:0;opacity:0;z-index:-1';
    document.body.appendChild(textArea);
    try {
      textArea.focus();
      textArea.select();
      if (textArea.setSelectionRange) textArea.setSelectionRange(0, 99999);
      const ok = document.execCommand('copy');
      ok ? resolve() : reject(new Error('copy failed'));
    } catch (err) {
      reject(err);
    } finally {
      document.body.removeChild(textArea);
    }
  });
};

const showToast = (message: string) => {
  const toast = document.createElement('div');
  toast.textContent = message;
  toast.style.cssText = `
    position: fixed; top: 24px; right: 24px;
    background: #1a3a4a; color: #f5f0e8;
    padding: 12px 20px; border-radius: 12px;
    box-shadow: 0 12px 30px -12px rgba(26,58,74,0.55);
    z-index: 10000; font-family: 'DM Sans', system-ui, sans-serif; font-size: 0.875rem;
  `;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2800);
};
</script>

<style scoped>
.stream-result {
  max-width: 960px;
  margin: 2rem auto 0;
  color: var(--wa-text-dark);
}

/* ---- Card frame ---- */
.result-card {
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 30px 60px -30px color-mix(in srgb, var(--wa-ocean) 40%, transparent);
}

.browser-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: color-mix(in srgb, var(--wa-sand) 65%, var(--wa-cream));
  border-bottom: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
}
.dot { width: 10px; height: 10px; border-radius: 50%; }
.dot-r { background: #e27763; }
.dot-y { background: #e3b34b; }
.dot-g { background: #87a590; }
.url-pill {
  margin-left: auto;
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.75rem;
  color: var(--wa-text-mid);
  background: var(--wa-warm);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  padding: 4px 12px;
  border-radius: 999px;
}

/* ---- Plan header ---- */
.plan-header {
  display: flex;
  justify-content: space-between;
  gap: 1.5rem;
  padding: 2rem 2rem 1.25rem;
  flex-wrap: wrap;
}
.plan-meta { display: flex; flex-direction: column; gap: 0.5rem; max-width: 640px; }
.eyebrow {
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--wa-terra);
}
.plan-title {
  font-family: var(--wa-font-serif);
  font-size: clamp(1.75rem, 2vw + 1rem, 2.5rem);
  color: var(--wa-ocean);
  letter-spacing: -0.01em;
  margin: 0;
  line-height: 1.15;
}
.plan-sub {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  margin: 0;
}

.plan-chips { display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: flex-start; }
.chip {
  display: inline-flex;
  align-items: center;
  padding: 0.375rem 0.75rem;
  border-radius: 999px;
  background: var(--wa-warm);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  color: var(--wa-text-dark);
}
.chip-mono {
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.6875rem;
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-color: var(--wa-ocean);
}

/* ---- Actions ---- */
.plan-actions {
  display: flex;
  gap: 0.625rem;
  padding: 0 2rem 1.25rem;
  flex-wrap: wrap;
}
.action-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 1.25rem;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--wa-ocean) 25%, transparent);
  background: var(--wa-warm);
  color: var(--wa-ocean);
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 150ms ease;
}
.action-button:hover:not(:disabled) { background: var(--wa-ocean); color: var(--wa-cream); }
.action-button.primary {
  background: var(--wa-ocean);
  color: var(--wa-cream);
  border-color: var(--wa-ocean);
}
.action-button.primary:hover:not(:disabled) { background: var(--wa-dusk); }
.action-button:disabled { opacity: 0.55; cursor: not-allowed; }
.spinner-mini {
  width: 14px;
  height: 14px;
  border: 2px solid color-mix(in srgb, currentColor 30%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}

/* ---- Timeline ---- */
.timeline {
  list-style: none;
  padding: 0 2rem 2rem;
  margin: 0;
  position: relative;
}
.timeline::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: calc(2rem + 18px);
  width: 2px;
  background: linear-gradient(180deg, var(--wa-terra) 0%, var(--wa-sage) 100%);
  border-radius: 999px;
  opacity: 0.5;
}
.day-block {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 1.25rem;
  padding: 1.25rem 0;
  position: relative;
}
.day-mark {
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
}
.day-number {
  font-family: var(--wa-font-sans);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--wa-cream);
  background: var(--wa-ocean);
  padding: 0.375rem 0.625rem;
  border-radius: 999px;
  position: relative;
  z-index: 1;
  white-space: nowrap;
  box-shadow: 0 4px 10px -3px color-mix(in srgb, var(--wa-ocean) 55%, transparent);
}
.day-body {
  background: var(--wa-warm);
  border-radius: 20px;
  padding: 1.25rem 1.5rem;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 50%, transparent);
}
.day-title {
  font-family: var(--wa-font-serif);
  font-size: 1.375rem;
  color: var(--wa-ocean);
  margin: 0 0 0.375rem;
  font-weight: 500;
  letter-spacing: -0.01em;
}
.day-overview {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  margin: 0 0 0.875rem;
}

.spot-list { list-style: none; padding: 0; margin: 0.5rem 0 0; display: flex; flex-direction: column; gap: 0.625rem; }
.spot {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 0.75rem;
  padding: 0.75rem 0.875rem;
  background: var(--wa-cream);
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 45%, transparent);
}
.spot-time {
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--wa-terra);
  align-self: flex-start;
  padding-top: 2px;
}
.spot-meat { display: flex; flex-direction: column; gap: 3px; }
.spot-name {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-dark);
  font-weight: 600;
}
.spot-addr, .spot-activity, .spot-notes {
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-text-mid);
  line-height: 1.45;
}
.spot-activity { color: var(--wa-sage); }

.day-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-top: 0.875rem;
  padding-top: 0.875rem;
  border-top: 1px dashed color-mix(in srgb, var(--wa-sand) 70%, transparent);
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  color: var(--wa-text-mid);
}
.day-foot em {
  font-style: normal;
  font-weight: 600;
  color: var(--wa-ocean);
  margin-right: 6px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  font-size: 0.6875rem;
}

.bullet { list-style: disc; padding-left: 1.25rem; margin: 0.5rem 0 0; font-family: var(--wa-font-sans); font-size: 0.9375rem; color: var(--wa-text-dark); }
.bullet li { margin: 4px 0; line-height: 1.5; }

.tips-block {
  margin: 0 2rem 2rem;
  padding: 1.25rem 1.5rem;
  background: color-mix(in srgb, var(--wa-sage) 18%, var(--wa-warm));
  border-radius: 18px;
  border: 1px solid color-mix(in srgb, var(--wa-sage) 30%, transparent);
}
.tips-title {
  font-family: var(--wa-font-serif);
  font-size: 1.125rem;
  color: var(--wa-ocean);
  margin: 0 0 0.5rem;
  font-weight: 500;
  font-style: italic;
}

/* ---- Fallback bubble ---- */
.ai-bubble {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: 0.875rem;
  padding: 1.5rem 2rem 2rem;
}
.ai-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--wa-ocean);
  color: var(--wa-cream);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--wa-font-serif);
  font-style: italic;
  font-weight: 500;
}
.stream-text {
  background: var(--wa-warm);
  border-radius: 18px;
  padding: 1.25rem 1.5rem;
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-dark);
  line-height: 1.65;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 45%, transparent);
}
.stream-text :deep(.markdown-h1),
.stream-text :deep(.markdown-h2),
.stream-text :deep(.markdown-h3) {
  font-family: var(--wa-font-serif);
  color: var(--wa-ocean);
  font-weight: 500;
  margin: 1rem 0 0.5rem;
  line-height: 1.25;
}
.stream-text :deep(.markdown-h1) { font-size: 1.5rem; }
.stream-text :deep(.markdown-h2) { font-size: 1.25rem; }
.stream-text :deep(.markdown-h3) { font-size: 1.0625rem; }
.stream-text :deep(.markdown-bold) { color: var(--wa-ocean); }
.stream-text :deep(.markdown-italic) { color: var(--wa-terra); font-style: italic; }
.stream-text :deep(.markdown-code) {
  background: var(--wa-cream);
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  color: var(--wa-terra);
  padding: 2px 6px;
  border-radius: 6px;
  font-family: 'SF Mono', Menlo, monospace;
  font-size: 0.85em;
}
.stream-text :deep(.markdown-list),
.stream-text :deep(.markdown-ordered-list) {
  background: var(--wa-cream);
  border-left: 3px solid var(--wa-terra);
  padding: 0.75rem 1rem;
  border-radius: 10px;
  margin: 0.75rem 0;
}
.stream-text :deep(.markdown-blockquote) {
  background: color-mix(in srgb, var(--wa-sage) 20%, var(--wa-warm));
  border-left: 3px solid var(--wa-sage);
  padding: 0.75rem 1rem;
  border-radius: 10px;
  color: var(--wa-text-mid);
  font-style: italic;
}
.stream-text :deep(.markdown-hr) {
  border: none;
  border-top: 1px dashed color-mix(in srgb, var(--wa-sand) 70%, transparent);
  margin: 1.25rem 0;
}
.stream-text :deep(.markdown-link) {
  color: var(--wa-terra);
  border-bottom: 1px solid color-mix(in srgb, var(--wa-terra) 40%, transparent);
  text-decoration: none;
}

/* ---- Error ---- */
.error-container {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 1.25rem;
  align-items: center;
  padding: 1.5rem;
  background: color-mix(in srgb, var(--wa-terra) 12%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-terra) 30%, transparent);
  border-radius: 20px;
}
.error-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--wa-terra);
  color: var(--wa-cream);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--wa-font-serif);
  font-size: 1.5rem;
  font-weight: 600;
}
.error-title {
  font-family: var(--wa-font-serif);
  font-size: 1.125rem;
  color: var(--wa-ocean);
  margin: 0;
  font-weight: 500;
}
.error-message {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  margin: 4px 0 0;
}
.retry-button {
  padding: 0.625rem 1.25rem;
  border-radius: 999px;
  background: var(--wa-terra);
  color: var(--wa-cream);
  border: none;
  font-family: var(--wa-font-sans);
  font-size: 0.8125rem;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}
.retry-button:hover { background: color-mix(in srgb, var(--wa-terra) 80%, #000); }

/* ---- Empty ---- */
.empty-state {
  text-align: center;
  padding: 3rem 1.5rem;
  background: var(--wa-cream);
  border: 1px dashed color-mix(in srgb, var(--wa-sand) 70%, transparent);
  border-radius: 24px;
}
.empty-tag {
  display: inline-block;
  font-family: var(--wa-font-sans);
  font-size: 0.6875rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--wa-terra);
  background: color-mix(in srgb, var(--wa-terra) 12%, transparent);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  margin-bottom: 0.75rem;
}
.empty-title {
  font-family: var(--wa-font-serif);
  font-size: 1.5rem;
  color: var(--wa-ocean);
  margin: 0 0 0.5rem;
  font-weight: 500;
}
.empty-desc {
  font-family: var(--wa-font-sans);
  font-size: 0.9375rem;
  color: var(--wa-text-mid);
  line-height: 1.55;
  max-width: 520px;
  margin: 0 auto;
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .plan-header { padding: 1.5rem 1.25rem 1rem; }
  .plan-actions { padding: 0 1.25rem 1rem; }
  .flights-block { padding: 0 1.25rem; }
  .cost-summary { margin: 12px 1.25rem 0; }
  .map-block { padding: 0 1.25rem 0.5rem; }
  .timeline { padding: 0 1.25rem 1.5rem; }
  .timeline::before { left: calc(1.25rem + 12px); }
  .day-block { grid-template-columns: 36px 1fr; gap: 0.75rem; }
  .day-number { font-size: 0.6875rem; padding: 0.25rem 0.5rem; }
  .day-body { padding: 1rem; border-radius: 16px; }
  .spot { grid-template-columns: 64px 1fr; }
  .ai-bubble { grid-template-columns: 36px 1fr; padding: 1.25rem; gap: 0.625rem; }
  .ai-avatar { width: 36px; height: 36px; }
}
</style>

<style scoped>
/* --- 항공권 / 비용 / 지도 / 라이브 패널 (2026-07 추가) --- */
.section-title {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 1.25rem 0 0.6rem;
  color: var(--wa-text, #1a1a1a);
}
.section-title .muted { font-weight: 400; font-size: 0.78rem; opacity: 0.6; margin-left: 6px; }

.flights-block { margin-top: 0.5rem; padding: 0 2rem; }
.flight-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 10px; }
.flight-card {
  border: 1px solid rgba(0, 0, 0, 0.09);
  border-radius: 12px;
  padding: 12px 14px;
  background: rgba(37, 99, 235, 0.03);
}
.flight-route { display: flex; align-items: center; gap: 8px; font-size: 0.95rem; }
.flight-badge {
  font-size: 0.72rem; padding: 2px 8px; border-radius: 999px;
  background: rgba(0, 0, 0, 0.06); color: #555;
}
.flight-badge.direct { background: rgba(5, 150, 105, 0.14); color: #047857; }
.flight-meta { display: flex; flex-wrap: wrap; gap: 10px; font-size: 0.8rem; color: #666; margin: 6px 0; }
.flight-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.flight-price { font-weight: 700; font-size: 1.05rem; color: #1a1a1a; }
.flight-book {
  text-decoration: none; font-size: 0.82rem; font-weight: 600;
  background: #2563eb; color: #fff; padding: 6px 14px; border-radius: 8px;
}
.flight-book:hover { background: #1d4ed8; }

.cost-summary {
  display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
  margin: 14px 2rem 0; padding: 12px 16px; border-radius: 12px;
  background: rgba(217, 119, 6, 0.08);
}
.cost-label { font-size: 0.82rem; color: #92400e; font-weight: 600; }
.cost-value { font-size: 1.25rem; font-weight: 800; color: #b45309; }
.cost-note { font-size: 0.75rem; color: #a16207; opacity: 0.85; }

.map-block { margin-top: 1.25rem; padding: 0 2rem 0.5rem; }

.generating-card { padding: 1.5rem; }
.gen-head { display: flex; align-items: center; gap: 14px; }
.gen-spinner {
  width: 26px; height: 26px; border-radius: 50%;
  border: 3px solid rgba(37, 99, 235, 0.2); border-top-color: #2563eb;
  animation: gen-spin 0.8s linear infinite; flex-shrink: 0;
}
@keyframes gen-spin { to { transform: rotate(360deg); } }
.gen-title { font-size: 1rem; font-weight: 700; margin: 0; }
.gen-sub { font-size: 0.8rem; color: #666; margin: 4px 0 0; display: flex; align-items: center; gap: 8px; }
.gen-count { opacity: 0.75; }
.gen-stream {
  margin-top: 14px; max-height: 180px; overflow: hidden;
  font-family: ui-monospace, monospace; font-size: 0.72rem; line-height: 1.5;
  color: #7a7a7a; white-space: pre-wrap; word-break: break-all;
  background: rgba(0, 0, 0, 0.03); border-radius: 10px; padding: 12px;
}
</style>

<style scoped>
.soft-fail { text-align: center; padding: 2.5rem 1.5rem; }
.sf-title { font-size: 1.15rem; font-weight: 700; margin: 0 0 0.5rem; color: var(--wa-ocean, #1a3b5c); }
.sf-desc { font-size: 0.9rem; color: #666; margin: 0 0 1.25rem; line-height: 1.5; }
</style>

<style scoped>
/* --- 생성 중 실시간 빌드업 --- */
.building-card { padding-bottom: 1.5rem; }
.build-header { padding-bottom: 1rem; }
.build-cursor { display: inline-block; margin-left: 2px; color: var(--wa-terra, #c1593a); animation: build-blink 1s steps(2) infinite; }
@keyframes build-blink { 0%,50% { opacity: 1; } 50.01%,100% { opacity: 0; } }
.build-count { color: var(--wa-text-mid, #666); }

.build-days { padding: 0 2rem; display: flex; flex-direction: column; gap: 0.75rem; }
.build-day {
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 14px; padding: 0.85rem 1rem; background: var(--wa-cream, #fff);
  animation: build-in 260ms ease;
}
@keyframes build-in { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: none; } }
.build-day-head { font-weight: 700; font-size: 0.92rem; color: var(--wa-ocean, #1a3b5c); margin-bottom: 0.4rem; }
.build-spots { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.3rem; }
.build-spots li { display: flex; gap: 0.6rem; font-size: 0.86rem; color: var(--wa-text-dark, #333); }
.bs-time { color: var(--wa-terra, #c1593a); font-weight: 600; min-width: 44px; }
.bs-name { flex: 1; }

.build-more {
  display: flex; align-items: center; gap: 0.5rem;
  font-size: 0.82rem; color: var(--wa-text-mid, #666); padding: 0.4rem 0.2rem;
}
.dot-flash {
  width: 8px; height: 8px; border-radius: 50%; background: var(--wa-terra, #c1593a);
  animation: build-blink 0.9s infinite;
}
.build-warming {
  display: flex; align-items: center; gap: 0.7rem;
  padding: 1.5rem 2rem; font-size: 0.9rem; color: var(--wa-text-mid, #666);
}
.build-spinner {
  width: 20px; height: 20px; border-radius: 50%;
  border: 3px solid color-mix(in srgb, var(--wa-ocean) 20%, transparent);
  border-top-color: var(--wa-ocean, #1a3b5c); animation: build-spin 0.8s linear infinite;
}
@keyframes build-spin { to { transform: rotate(360deg); } }
</style>

<style scoped>
/* --- 여행 개요 --- */
.trip-info {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 0.75rem;
  padding: 0 2rem; margin-top: 0.5rem;
}
.ti-card {
  background: color-mix(in srgb, var(--wa-sand) 22%, var(--wa-cream));
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 14px; padding: 0.9rem 1rem;
}
.ti-card h4 { margin: 0 0 0.5rem; font-size: 0.9rem; color: var(--wa-ocean, #1a3b5c); }
.ti-card p { margin: 0; font-size: 0.86rem; line-height: 1.5; color: var(--wa-text-dark, #333); }
.ti-pack { margin: 0; padding-left: 1.1rem; font-size: 0.84rem; line-height: 1.6; }
.ti-cost { list-style: none; margin: 0; padding: 0; }
.ti-cost li { display: flex; justify-content: space-between; font-size: 0.85rem; padding: 2px 0; }
.ti-cost li span { color: var(--wa-text-mid, #666); }

/* --- 일자별 책자 스프레드 --- */
.booklet { padding: 1.25rem 2rem 0; display: flex; flex-direction: column; gap: 1.25rem; }
.spread {
  display: grid; grid-template-columns: 1fr 1fr; gap: 1.1rem;
  border: 1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent);
  border-radius: 18px; overflow: hidden; background: var(--wa-cream, #fff);
}
.spread-left { padding: 1.25rem 1.35rem; border-right: 1px dashed color-mix(in srgb, var(--wa-sand) 60%, transparent); }
.spread-right { padding: 1rem; background: color-mix(in srgb, var(--wa-sand) 14%, var(--wa-cream)); display: flex; flex-direction: column; gap: 0.75rem; }

.spread-daybadge {
  display: inline-block; background: var(--wa-ocean, #1a3b5c); color: #fff;
  font-size: 0.72rem; font-weight: 700; padding: 3px 10px; border-radius: 999px; margin-bottom: 0.4rem;
}
.spread-title { margin: 0 0 0.4rem; font-size: 1.1rem; color: var(--wa-ocean, #1a3b5c); }
.spread-overview { margin: 0 0 0.85rem; font-size: 0.88rem; line-height: 1.55; color: var(--wa-text-mid, #555); }

.spread-facts { display: flex; flex-direction: column; gap: 0.5rem; margin-bottom: 0.9rem; }
.fact { display: flex; gap: 0.55rem; font-size: 0.83rem; }
.fact-ic { flex-shrink: 0; }
.fact b { display: block; font-size: 0.72rem; color: var(--wa-text-light, #999); font-weight: 600; }
.fact span { color: var(--wa-text-dark, #333); line-height: 1.4; }

.spread-schedule { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 0.65rem; }
.spread-schedule li { display: flex; gap: 0.7rem; }
.ss-time { flex-shrink: 0; min-width: 46px; font-weight: 700; font-size: 0.82rem; color: var(--wa-terra, #c1593a); }
.ss-body { display: flex; flex-direction: column; gap: 1px; }
.ss-body strong { font-size: 0.9rem; color: var(--wa-text-dark, #222); }
.ss-act { font-size: 0.8rem; color: var(--wa-text-mid, #666); }
.ss-note { font-size: 0.76rem; color: var(--wa-text-light, #999); }
.spread-tip { margin: 0.9rem 0 0; font-size: 0.82rem; color: var(--wa-text-mid, #555); background: color-mix(in srgb, var(--wa-amber) 12%, transparent); padding: 0.5rem 0.7rem; border-radius: 8px; }

.spread-nomap { display: flex; align-items: center; justify-content: center; height: 180px; border-radius: 12px; background: color-mix(in srgb, var(--wa-sand) 30%, transparent); font-size: 0.82rem; color: var(--wa-text-light, #999); }
.spread-places .sp-title { font-size: 0.75rem; font-weight: 700; color: var(--wa-text-mid, #666); letter-spacing: 0.04em; }
.spread-places ol { list-style: none; margin: 0.5rem 0 0; padding: 0; display: flex; flex-direction: column; gap: 0.35rem; }
.spread-places li { display: flex; align-items: center; gap: 0.5rem; font-size: 0.84rem; color: var(--wa-text-dark, #333); }
.sp-idx { flex-shrink: 0; width: 20px; height: 20px; border-radius: 50%; background: var(--wa-ocean, #1a3b5c); color: #fff; font-size: 0.7rem; display: inline-flex; align-items: center; justify-content: center; }

@media (max-width: 760px) {
  .trip-info, .booklet { padding-left: 1.25rem; padding-right: 1.25rem; }
  .spread { grid-template-columns: 1fr; }
  .spread-left { border-right: none; border-bottom: 1px dashed color-mix(in srgb, var(--wa-sand) 60%, transparent); }
}
</style>

<style scoped>
.flight-badge.trip { background: rgba(37, 99, 235, 0.12); color: #1d4ed8; }
.flight-dur { color: #666; }
.fp-unit { font-size: 0.75rem; font-weight: 500; color: var(--wa-text-mid, #888); }
</style>

<style scoped>
/* 타임테이블 */
.tt-label, .mb-label, .dc-label { font-size: 0.78rem; font-weight: 700; color: var(--wa-ocean,#1a3b5c); margin: 0.9rem 0 0.4rem; display:block; }
.timetable { list-style: none; margin: 0; padding: 0; border-left: 2px solid color-mix(in srgb, var(--wa-sand) 70%, transparent); }
.tt-row { display: flex; gap: 0.7rem; padding: 0.35rem 0 0.35rem 0.8rem; position: relative; }
.tt-row::before { content:''; position:absolute; left:-5px; top:0.6rem; width:8px; height:8px; border-radius:50%; background: var(--wa-terra,#c1593a); }
.tt-time { flex-shrink:0; min-width:52px; font-weight:700; font-size:0.8rem; color: var(--wa-terra,#c1593a); display:flex; flex-direction:column; }
.tt-time em { font-style:normal; font-weight:400; font-size:0.68rem; color: var(--wa-text-light,#999); }
.tt-body { display:flex; flex-direction:column; gap:1px; }
.tt-body strong { font-size:0.9rem; }
.tt-act { font-size:0.8rem; color: var(--wa-text-mid,#666); }
.tt-note { font-size:0.75rem; color: var(--wa-text-light,#999); }

/* 식당 */
.meals-block { margin-top: 0.4rem; }
.meal-row { display:flex; gap:0.6rem; padding:0.4rem 0; border-top:1px dashed color-mix(in srgb, var(--wa-sand) 55%, transparent); }
.meal-type { flex-shrink:0; min-width:44px; font-size:0.72rem; font-weight:600; color:#fff; background: var(--wa-sage,#7a9b76); border-radius:6px; height:22px; display:inline-flex; align-items:center; justify-content:center; padding:0 6px; }
.meal-body { display:flex; flex-direction:column; gap:1px; }
.meal-body strong { font-size:0.88rem; }
.meal-menu { font-size:0.8rem; color: var(--wa-text-mid,#666); }
.meal-sub { font-size:0.74rem; color: var(--wa-text-light,#999); display:flex; gap:0.6rem; flex-wrap:wrap; }
.meal-price { color: var(--wa-terra,#c1593a); font-weight:600; }

/* 일자 비용 */
.daycost { margin-top: 0.9rem; background: color-mix(in srgb, var(--wa-amber) 10%, transparent); border-radius: 10px; padding: 0.6rem 0.8rem; }
.dc-items { list-style:none; margin:0.3rem 0 0; padding:0; }
.dc-items li { display:flex; justify-content:space-between; font-size:0.82rem; padding:1px 0; }
.dc-items li span { color: var(--wa-text-mid,#666); }
.dc-str { font-size:0.82rem; color: var(--wa-text-dark,#333); }

/* 숙소 */
.stay-block { padding: 0 2rem; margin-top: 0.5rem; }
.stay-card { border:1px solid color-mix(in srgb, var(--wa-sand) 55%, transparent); border-radius:14px; padding: 0.9rem 1.1rem; background: color-mix(in srgb, var(--wa-ocean) 4%, var(--wa-cream)); }
.stay-name { font-size:1rem; }
.stay-meta { font-size:0.82rem; color: var(--wa-text-mid,#666); margin-left:0.5rem; }
.stay-foot { display:flex; align-items:center; justify-content:space-between; margin-top:0.5rem; }
.stay-price { font-weight:700; color: var(--wa-terra,#c1593a); }
.stay-book { text-decoration:none; font-size:0.82rem; font-weight:600; background: var(--wa-ocean,#1a3b5c); color:#fff; padding:6px 14px; border-radius:8px; }
@media (max-width: 760px) { .stay-block { padding-left:1.25rem; padding-right:1.25rem; } }
</style>
