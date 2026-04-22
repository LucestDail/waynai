<template>
  <div class="tourist-info-view">
    <div class="container">
      <div class="header">
        <h1 class="title">관광 정보 조회</h1>
        <p class="subtitle">한국관광공사 공공데이터 기준으로 지역별 인기 관광지를 조회합니다</p>
      </div>

      <div class="search-section">
        <div class="search-form">
          <div class="input-group">
            <label for="areaSearch" class="input-label">지역 검색</label>
            <input
              id="areaSearch"
              v-model="searchArea"
              type="text"
              class="search-input"
              placeholder="예시: 서울, 부산, 제주, 강원"
              @keyup.enter="searchTouristInfo"
            />
          </div>

          <div class="search-options">
            <div class="option-group">
              <label for="sigunguFilter" class="option-label">시군구</label>
              <select
                id="sigunguFilter"
                v-model="selectedSigunguKey"
                class="option-select"
                :disabled="sigunguOptions.length === 0"
                @change="loadSpotsForSelected"
              >
                <option v-if="sigunguOptions.length === 0" value="">먼저 지역을 검색하세요</option>
                <option
                  v-for="sg in sigunguOptions"
                  :key="sg.areaCode + '-' + sg.sigunguCode"
                  :value="sg.areaCode + '-' + sg.sigunguCode"
                >
                  {{ sg.areaName }} · {{ sg.sigunguName }}
                </option>
              </select>
            </div>

            <div class="option-group">
              <label for="sortBy" class="option-label">정렬</label>
              <select
                id="sortBy"
                v-model="sortBy"
                class="option-select"
              >
                <option value="rank">추천순 (hubRank)</option>
                <option value="name">이름순</option>
              </select>
            </div>
          </div>

          <div class="search-actions">
            <button
              @click="searchTouristInfo"
              class="search-button"
              :disabled="!searchArea.trim() || isLoading"
            >
              {{ isLoading ? '조회 중…' : '검색' }}
            </button>
          </div>
        </div>

        <!-- 에러 상태 -->
        <div v-if="errorMessage" class="error-banner">
          <strong>오류</strong> · {{ errorMessage }}
        </div>

        <!-- 로딩 상태 -->
        <div v-if="isLoading" class="loading-container">
          <div class="loading-spinner"></div>
          <p class="loading-text">{{ loadingMessage }}</p>
        </div>

        <!-- 결과 표시 -->
        <div v-if="sortedSpots.length > 0" class="results-section">
          <h3 class="results-title">
            {{ currentSigunguLabel }} · 검색 결과 {{ sortedSpots.length }}건
            <span v-if="totalCount > sortedSpots.length" class="results-subtotal">
              (전체 {{ totalCount }}건 중)
            </span>
          </h3>
          <div class="tourist-grid">
            <div
              v-for="spot in sortedSpots"
              :key="spot.hubTatsCd"
              class="tourist-card"
            >
              <div class="card-header">
                <h4 class="spot-name">{{ spot.hubTatsNm }}</h4>
                <span class="spot-category">{{ categoryOf(spot) }}</span>
              </div>
              <div class="card-content">
                <div class="spot-details">
                  <span class="detail-item">
                    <strong>지역:</strong> {{ spot.areaNm }} {{ spot.signguNm }}
                  </span>
                  <span class="detail-item">
                    <strong>좌표:</strong> {{ formatCoord(spot.mapY) }}, {{ formatCoord(spot.mapX) }}
                  </span>
                  <span class="detail-item">
                    <strong>기준월:</strong> {{ formatBaseYm(spot.baseYm) }}
                  </span>
                  <a
                    class="detail-item map-link"
                    :href="mapLink(spot)"
                    target="_blank"
                    rel="noopener noreferrer"
                  >지도에서 보기 ↗</a>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 빈 결과 -->
        <div v-else-if="hasSearched && !isLoading && !errorMessage" class="empty-results">
          <p>검색 결과가 없습니다.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import axios from 'axios';
import touristInfoService, {
  type AreaCodeDto,
  type TouristSpotDto,
} from '@/services/touristInfoService';

const searchArea = ref('');
const sortBy = ref<'rank' | 'name'>('rank');
const isLoading = ref(false);
const hasSearched = ref(false);
const errorMessage = ref('');
const loadingMessage = ref('관광 정보를 검색하고 있습니다...');

const sigunguOptions = ref<AreaCodeDto[]>([]);
const selectedSigunguKey = ref('');
const spots = ref<TouristSpotDto[]>([]);
const totalCount = ref(0);

const currentSigunguLabel = computed(() => {
  const sg = sigunguOptions.value.find(
    (s) => `${s.areaCode}-${s.sigunguCode}` === selectedSigunguKey.value,
  );
  return sg ? `${sg.areaName} ${sg.sigunguName}` : '';
});

const sortedSpots = computed(() => {
  const list = [...spots.value];
  if (sortBy.value === 'name') {
    list.sort((a, b) => (a.hubTatsNm || '').localeCompare(b.hubTatsNm || '', 'ko'));
  } else {
    list.sort((a, b) => Number(a.hubRank || 9999) - Number(b.hubRank || 9999));
  }
  return list;
});

const categoryOf = (spot: TouristSpotDto): string =>
  spot.hubCtgrySclsNm || spot.hubCtgryMclsNm || spot.hubCtgryLclsNm || '기타';

const formatCoord = (v: string | null | undefined): string => {
  const n = Number(v);
  return Number.isFinite(n) ? n.toFixed(5) : '-';
};

const formatBaseYm = (ym: string | null | undefined): string => {
  if (!ym || ym.length !== 6) return ym ?? '-';
  return `${ym.slice(0, 4)}.${ym.slice(4)}`;
};

const mapLink = (spot: TouristSpotDto): string => {
  const q = encodeURIComponent(spot.hubTatsNm);
  return `https://map.naver.com/p/search/${q}`;
};

const extractErrorMessage = (err: unknown): string => {
  if (axios.isAxiosError(err)) {
    const body = err.response?.data as { resultMsg?: string; message?: string } | undefined;
    return body?.resultMsg || body?.message || err.message;
  }
  if (err instanceof Error) return err.message;
  return '알 수 없는 오류';
};

const searchTouristInfo = async () => {
  const q = searchArea.value.trim();
  if (!q) return;

  isLoading.value = true;
  hasSearched.value = true;
  errorMessage.value = '';
  spots.value = [];
  totalCount.value = 0;
  loadingMessage.value = `${q} 지역의 시군구 정보를 불러오는 중...`;

  try {
    const areas = await touristInfoService.searchAreaByName(q);
    sigunguOptions.value = areas;

    if (areas.length === 0) {
      selectedSigunguKey.value = '';
      errorMessage.value = `"${q}" 로 매칭되는 지역이 없습니다. (예: 서울, 부산, 제주, 강원, 경기 등)`;
      return;
    }

    selectedSigunguKey.value = `${areas[0].areaCode}-${areas[0].sigunguCode}`;
    await loadSpotsForSelected();
  } catch (err) {
    errorMessage.value = extractErrorMessage(err);
  } finally {
    isLoading.value = false;
  }
};

const loadSpotsForSelected = async () => {
  if (!selectedSigunguKey.value) return;
  const [areaCode, sigunguCode] = selectedSigunguKey.value.split('-');
  if (!areaCode || !sigunguCode) return;

  isLoading.value = true;
  errorMessage.value = '';
  loadingMessage.value = `${currentSigunguLabel.value} 관광지 정보를 불러오는 중...`;

  try {
    const res = await touristInfoService.getSpots(areaCode, sigunguCode, 1, 20);
    if (!res.success) {
      throw new Error(res.resultMsg || '조회 실패');
    }
    spots.value = res.items ?? [];
    totalCount.value = res.totalCount ?? spots.value.length;
  } catch (err) {
    spots.value = [];
    totalCount.value = 0;
    errorMessage.value = extractErrorMessage(err);
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* ========== TouristInfoView — Material Design 3 ========== */
.tourist-info-view {
  min-height: calc(100vh - 64px);
  background: var(--m3-background);
  padding: 2.5rem 1.25rem 4rem;
  color: var(--m3-on-background);
}

.container { max-width: 1200px; margin: 0 auto; }

.header {
  text-align: center;
  margin-bottom: 2.5rem;
  padding: 2rem 1rem 1.5rem;
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--m3-primary-container) 60%, var(--m3-background)) 0%,
    var(--m3-background) 100%
  );
  border-radius: var(--m3-shape-xl);
}
.title {
  font: var(--m3-display-small);
  color: var(--m3-on-surface);
  margin: 0 0 0.5rem;
  letter-spacing: -0.01em;
}
.subtitle {
  font: var(--m3-body-large);
  color: var(--m3-on-surface-variant);
  margin: 0;
}

.search-section {
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-xl);
  padding: clamp(1.5rem, 2.5vw, 2.5rem);
  box-shadow: var(--m3-elev-1);
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  margin-bottom: 1.5rem;
}
.input-group, .option-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.input-label, .option-label {
  font: var(--m3-label-large);
  color: var(--m3-on-surface-variant);
}

.search-input,
.option-select {
  width: 100%;
  padding: 0.875rem 1rem;
  border: 1px solid var(--m3-outline-variant);
  border-radius: var(--m3-shape-sm);
  background: var(--m3-surface-container-lowest);
  color: var(--m3-on-surface);
  font: var(--m3-body-large);
  transition: border-color var(--m3-motion-short), box-shadow var(--m3-motion-short);
}
.search-input:hover,
.option-select:hover { border-color: var(--m3-outline); }
.search-input:focus,
.option-select:focus {
  outline: none;
  border-color: var(--m3-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--m3-primary) 18%, transparent);
}

.search-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.search-actions { display: flex; justify-content: center; padding-top: 0.5rem; }

.search-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  min-height: 44px;
  padding: 0 2rem;
  border-radius: var(--m3-shape-full);
  background: var(--m3-primary);
  color: var(--m3-on-primary);
  font: var(--m3-label-large);
  letter-spacing: 0.02em;
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--m3-motion-short);
}
.search-button::before {
  content: '';
  position: absolute; inset: 0;
  background: currentColor;
  opacity: 0;
  transition: opacity var(--m3-motion-short);
  pointer-events: none;
}
.search-button:hover:not(:disabled) { box-shadow: var(--m3-elev-1); }
.search-button:hover:not(:disabled)::before { opacity: var(--m3-state-hover); }
.search-button:active:not(:disabled)::before { opacity: var(--m3-state-pressed); }
.search-button:disabled { opacity: 0.38; cursor: not-allowed; }

/* Loading */
.loading-container { text-align: center; padding: 2.5rem 1rem; }
.loading-spinner {
  width: 44px; height: 44px;
  border: 4px solid color-mix(in srgb, var(--m3-primary) 18%, transparent);
  border-top-color: var(--m3-primary);
  border-radius: 50%;
  animation: spin 900ms linear infinite;
  margin: 0 auto 0.75rem;
}
.loading-text { color: var(--m3-on-surface-variant); font: var(--m3-body-large); }

.error-banner {
  margin-top: 1rem;
  padding: 0.875rem 1rem;
  border-radius: var(--m3-shape-sm);
  background: color-mix(in srgb, var(--m3-error-container, #ffdad6) 70%, transparent);
  color: var(--m3-on-error-container, #410002);
  font: var(--m3-body-medium);
  border-left: 4px solid var(--m3-error, #b3261e);
}

.results-section { margin-top: 2rem; }
.results-title {
  font: var(--m3-title-large);
  color: var(--m3-on-surface);
  margin: 0 0 1.25rem;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 0.5rem;
}
.results-subtotal {
  font: var(--m3-body-medium);
  color: var(--m3-on-surface-variant);
}

.map-link {
  color: var(--m3-primary);
  text-decoration: none;
  margin-top: 0.25rem;
}
.map-link:hover { text-decoration: underline; }

.tourist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}

/* M3 Elevated card */
.tourist-card {
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-lg);
  padding: 1.5rem;
  box-shadow: var(--m3-elev-1);
  transition: box-shadow var(--m3-motion-medium), transform var(--m3-motion-medium);
  cursor: pointer;
}
.tourist-card:hover {
  box-shadow: var(--m3-elev-3);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.spot-name {
  font: var(--m3-title-medium);
  color: var(--m3-on-surface);
  margin: 0;
  flex: 1;
}

/* Tonal chip */
.spot-category {
  background: var(--m3-secondary-container);
  color: var(--m3-on-secondary-container);
  padding: 0.25rem 0.625rem;
  border-radius: var(--m3-shape-sm);
  font: var(--m3-label-medium);
  flex-shrink: 0;
}

.card-content { color: var(--m3-on-surface-variant); }

.spot-description {
  margin-bottom: 0.875rem;
  line-height: 1.5;
  font: var(--m3-body-medium);
}
.spot-details {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--m3-outline-variant);
}
.detail-item { font: var(--m3-body-small); color: var(--m3-on-surface-variant); }
.detail-item strong { color: var(--m3-on-surface); font-weight: 600; }

.empty-results {
  text-align: center;
  padding: 3rem 1rem;
  color: var(--m3-on-surface-variant);
  font: var(--m3-body-large);
  background: var(--m3-surface-container);
  border-radius: var(--m3-shape-lg);
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .tourist-info-view { padding: 1.25rem 0.75rem 3rem; }
  .header { margin-bottom: 1.5rem; padding: 1.5rem 1rem; }
  .search-section { padding: 1.5rem 1.25rem; }
  .search-options { grid-template-columns: 1fr; }
  .tourist-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .search-section { padding: 1.25rem 1rem; border-radius: var(--m3-shape-lg); }
  .tourist-card { padding: 1.25rem; }
}
</style> 