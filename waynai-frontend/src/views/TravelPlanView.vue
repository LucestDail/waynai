<template>
  <div class="travel-plan-view">
    <section class="intro">
      <span class="eyebrow">wayn · Plan Studio</span>
      <h1 class="title">
        구체적으로 설계하는 <em>맞춤 여행</em>
      </h1>
      <p class="subtitle">
        목적지, 일정, 테마, 예산을 알려주시면 관광공사·네이버 RAG와 Gemini 3 핫스왑이
        실시간으로 계획을 생성합니다.
      </p>
    </section>

    <section class="plan-card">
      <form class="plan-form" @submit.prevent="generateTravelPlan">
        <div class="form-section">
          <div class="section-head">
            <span class="section-bar"></span>
            <h3>목적지 정보</h3>
          </div>
          <div class="form-row">
            <label class="form-group">
              <span>지역</span>
              <select v-model="selectedArea" @change="updateSigunguOptions" class="form-select">
                <option value="">지역을 선택하세요</option>
                <option v-for="a in areaList" :key="a.value" :value="a.value">{{ a.label }}</option>
              </select>
            </label>
            <label class="form-group">
              <span>시군구</span>
              <select v-model="selectedSigungu" class="form-select" :disabled="!sigunguOptions.length">
                <option value="">시군구를 선택하세요</option>
                <option v-for="sigungu in sigunguOptions" :key="sigungu" :value="sigungu">
                  {{ sigungu }}
                </option>
              </select>
            </label>
          </div>
        </div>

        <div class="form-section">
          <div class="section-head">
            <span class="section-bar"></span>
            <h3>여행 정보</h3>
          </div>
          <div class="form-row">
            <label class="form-group">
              <span>여행 일정</span>
              <select v-model="selectedDuration" class="form-select">
                <option value="">일정을 선택하세요</option>
                <option value="1">당일치기</option>
                <option value="2">1박 2일</option>
                <option value="3">2박 3일</option>
                <option value="4">3박 4일</option>
              </select>
            </label>
            <label class="form-group">
              <span>여행 테마</span>
              <select v-model="selectedTheme" class="form-select">
                <option value="">테마를 선택하세요</option>
                <option value="문화">문화 / 역사</option>
                <option value="자연">자연 / 풍경</option>
                <option value="음식">음식 / 맛집</option>
                <option value="쇼핑">쇼핑 / 시장</option>
                <option value="레저">레저 / 액티비티</option>
                <option value="힐링">힐링 / 휴식</option>
              </select>
            </label>
          </div>
          <div class="form-row form-row--single">
            <div class="form-group">
              <span>이동 수단</span>
              <div class="chip-group">
                <label v-for="opt in transportationOptions" :key="opt" class="chip-option">
                  <input type="checkbox" v-model="transportation" :value="opt" />
                  <span>{{ opt }}</span>
                </label>
              </div>
            </div>
          </div>
          <div class="form-row form-row--single">
            <label class="form-group">
              <span>키워드</span>
              <input
                v-model="keywords"
                type="text"
                class="form-input"
                placeholder="예: 야경, 해산물, 카페, 벚꽃"
              />
            </label>
          </div>
        </div>

        <div class="form-section">
          <div class="section-head">
            <span class="section-bar"></span>
            <h3>추가 정보</h3>
          </div>
          <div class="form-row">
            <label class="form-group">
              <span>예산</span>
              <select v-model="selectedBudget" class="form-select">
                <option value="">예산을 선택하세요</option>
                <option value="저렴">저렴 (10만원 이하)</option>
                <option value="보통">보통 (10-30만원)</option>
                <option value="고급">고급 (30만원 이상)</option>
              </select>
            </label>
            <label class="form-group">
              <span>동반자</span>
              <select v-model="selectedCompanion" class="form-select">
                <option value="">동반자를 선택하세요</option>
                <option value="혼자">혼자</option>
                <option value="커플">커플</option>
                <option value="가족">가족</option>
                <option value="친구">친구</option>
                <option value="단체">단체</option>
              </select>
            </label>
          </div>
        </div>

        <div class="form-actions">
          <button
            type="submit"
            class="submit-button"
            :disabled="!isFormValid || streamState.isStreaming"
          >
            <span v-if="streamState.isStreaming" class="spinner"></span>
            <span v-else>여행 계획 생성하기</span>
          </button>
          <p class="hint">필수: 지역 · 시군구 · 일정 · 테마</p>
        </div>
      </form>
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
import { computed, onUnmounted, ref } from 'vue';

const streamStore = useStreamStore();
const streamState = streamStore.state;

const selectedArea = ref('');
const selectedSigungu = ref('');
const selectedDuration = ref('');
const selectedTheme = ref('');
const selectedBudget = ref('');
const selectedCompanion = ref('');
const transportation = ref<string[]>([]);
const keywords = ref('');

const transportationOptions = ['기차', '자동차', '도보', '전철', '버스', '택시'];

const areaList = [
  { value: '서울', label: '서울' },
  { value: '부산', label: '부산' },
  { value: '대구', label: '대구' },
  { value: '인천', label: '인천' },
  { value: '광주', label: '광주' },
  { value: '대전', label: '대전' },
  { value: '울산', label: '울산' },
  { value: '세종', label: '세종' },
  { value: '경기', label: '경기도' },
  { value: '강원', label: '강원도' },
  { value: '충북', label: '충청북도' },
  { value: '충남', label: '충청남도' },
  { value: '전북', label: '전라북도' },
  { value: '전남', label: '전라남도' },
  { value: '경북', label: '경상북도' },
  { value: '경남', label: '경상남도' },
  { value: '제주', label: '제주도' },
];

const sigunguOptions = ref<string[]>([]);

const sigunguData: Record<string, string[]> = {
  '서울': ['종로구', '중구', '용산구', '성동구', '광진구', '동대문구', '중랑구', '성북구', '강북구', '도봉구', '노원구', '은평구', '서대문구', '마포구', '양천구', '강서구', '구로구', '금천구', '영등포구', '동작구', '관악구', '서초구', '강남구', '송파구', '강동구'],
  '부산': ['부산중구', '부산서구', '부산동구', '영도구', '부산진구', '동래구', '부산남구', '부산북구', '해운대구', '사하구', '금정구', '부산강서구', '연제구', '수영구', '사상구', '기장군'],
  '대구': ['대구중구', '대구동구', '대구서구', '대구남구', '대구북구', '수성구', '달서구', '달성군', '군위군'],
  '인천': ['인천중구', '인천동구', '미추홀구', '연수구', '인천남동구', '부평구', '계양구', '인천서구', '강화군', '옹진군'],
  '광주': ['광주동구', '광주서구', '광주남구', '광주북구', '광산구'],
  '대전': ['대전동구', '대전중구', '대전서구', '유성구', '대덕구'],
  '울산': ['울산중구', '울산남구', '울산동구', '울산북구', '울주군'],
  '세종': ['세종특별자치시'],
  '경기': ['수원시', '성남시', '의정부시', '안양시', '부천시', '광명시', '평택시', '동두천시', '안산시', '고양시', '과천시', '구리시', '남양주시', '오산시', '시흥시', '군포시', '의왕시', '하남시', '용인시', '파주시', '이천시', '안성시', '김포시', '화성시', '광주시', '양주시', '포천시', '여주시', '연천군', '가평군', '양평군'],
  '강원': ['춘천시', '원주시', '강릉시', '동해시', '태백시', '속초시', '삼척시', '홍천군', '횡성군', '영월군', '평창군', '정선군', '철원군', '화천군', '양구군', '인제군', '고성군', '양양군'],
  '충북': ['청주시', '충주시', '제천시', '보은군', '옥천군', '영동군', '증평군', '진천군', '괴산군', '음성군', '단양군'],
  '충남': ['천안시', '공주시', '보령시', '아산시', '서산시', '논산시', '계룡시', '당진시', '금산군', '부여군', '서천군', '청양군', '홍성군', '예산군', '태안군'],
  '전북': ['전주시', '군산시', '익산시', '정읍시', '남원시', '김제시', '완주군', '진안군', '무주군', '장수군', '임실군', '순창군', '고창군', '부안군'],
  '전남': ['목포시', '여수시', '순천시', '나주시', '광양시', '담양군', '곡성군', '구례군', '고흥군', '보성군', '화순군', '장흥군', '강진군', '해남군', '영암군', '무안군', '함평군', '영광군', '장성군', '완도군', '진도군', '신안군'],
  '경북': ['포항시', '경주시', '김천시', '안동시', '구미시', '영주시', '영천시', '상주시', '문경시', '경산시', '군위군', '의성군', '청송군', '영양군', '영덕군', '청도군', '고령군', '성주군', '칠곡군', '예천군', '봉화군', '울진군', '울릉군'],
  '경남': ['창원시', '진주시', '통영시', '사천시', '김해시', '밀양시', '거제시', '양산시', '의령군', '함안군', '창녕군', '고성군', '남해군', '하동군', '산청군', '함양군', '거창군', '합천군'],
  '제주': ['제주시', '서귀포시'],
};

const updateSigunguOptions = () => {
  sigunguOptions.value = selectedArea.value && sigunguData[selectedArea.value]
    ? sigunguData[selectedArea.value]
    : [];
  selectedSigungu.value = '';
};

const isFormValid = computed(() =>
  !!(selectedArea.value && selectedSigungu.value && selectedDuration.value && selectedTheme.value)
);

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

const durationLabel: Record<string, string> = {
  '1': '당일치기',
  '2': '1박 2일',
  '3': '2박 3일',
  '4': '3박 4일',
};

const buildQuery = () => {
  const parts = [
    selectedArea.value,
    selectedSigungu.value,
    durationLabel[selectedDuration.value] ?? '',
    selectedTheme.value,
    selectedBudget.value ? `${selectedBudget.value} 예산` : '',
    selectedCompanion.value ? `${selectedCompanion.value} 동반` : '',
    transportation.value.length ? transportation.value.join('·') : '',
    keywords.value,
  ];
  return parts.filter(Boolean).join(' ').trim();
};

const generateTravelPlan = async () => {
  if (!isFormValid.value || streamState.isStreaming) return;
  const q = buildQuery();
  if (!q) return;
  await streamStore.startTravelPlanStream(q);
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
