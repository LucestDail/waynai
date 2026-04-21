<template>
  <div class="tourist-info-view">
    <div class="container">
      <div class="header">
        <h1 class="title">관광 정보 조회</h1>
        <p class="subtitle">지역별 관광지 정보를 검색하고 상세 정보를 확인하세요</p>
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
              placeholder="예시: 서울, 부산, 제주도"
              @keyup.enter="searchTouristInfo"
            />
          </div>

          <div class="search-options">
            <div class="option-group">
              <label for="categoryFilter" class="option-label">카테고리</label>
              <select
                id="categoryFilter"
                v-model="selectedCategory"
                class="option-select"
              >
                <option value="">전체</option>
                <option value="문화">문화</option>
                <option value="자연">자연</option>
                <option value="쇼핑">쇼핑</option>
                <option value="음식">음식</option>
                <option value="레저">레저</option>
              </select>
            </div>

            <div class="option-group">
              <label for="sortBy" class="option-label">정렬</label>
              <select
                id="sortBy"
                v-model="sortBy"
                class="option-select"
              >
                <option value="name">이름순</option>
                <option value="popularity">인기순</option>
                <option value="rating">평점순</option>
              </select>
            </div>
          </div>

          <div class="search-actions">
            <button
              @click="searchTouristInfo"
              class="search-button"
              :disabled="!searchArea.trim()"
            >
              검색
            </button>
          </div>
        </div>

        <!-- 로딩 상태 -->
        <div v-if="isLoading" class="loading-container">
          <div class="loading-spinner"></div>
          <p class="loading-text">관광 정보를 검색하고 있습니다...</p>
        </div>

        <!-- 결과 표시 -->
        <div v-if="touristInfo.length > 0" class="results-section">
          <h3 class="results-title">검색 결과 ({{ touristInfo.length }}개)</h3>
          <div class="tourist-grid">
            <div
              v-for="(spot, index) in touristInfo"
              :key="index"
              class="tourist-card"
            >
              <div class="card-header">
                <h4 class="spot-name">{{ spot.name }}</h4>
                <span class="spot-category">{{ spot.category }}</span>
              </div>
              <div class="card-content">
                <p class="spot-description">{{ spot.description }}</p>
                <div class="spot-details">
                  <span class="detail-item">
                    <strong>지역:</strong> {{ spot.area }}
                  </span>
                  <span class="detail-item">
                    <strong>주소:</strong> {{ spot.address }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 빈 결과 -->
        <div v-else-if="hasSearched && !isLoading" class="empty-results">
          <p>검색 결과가 없습니다.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const searchArea = ref('');
const selectedCategory = ref('');
const sortBy = ref('name');
const isLoading = ref(false);
const hasSearched = ref(false);

// 임시 데이터 (실제로는 API 호출)
const touristInfo = ref([
  {
    name: '경복궁',
    category: '문화',
    description: '조선왕조의 정궁으로 아름다운 전통 건축물을 감상할 수 있습니다.',
    area: '서울 종로구',
    address: '서울특별시 종로구 사직로 161'
  },
  {
    name: '해운대 해수욕장',
    category: '자연',
    description: '부산의 대표적인 해수욕장으로 아름다운 해변과 맑은 바다를 즐길 수 있습니다.',
    area: '부산 해운대구',
    address: '부산광역시 해운대구 해운대해변로 264'
  }
]);

const searchTouristInfo = async () => {
  if (!searchArea.value.trim()) return;
  
  isLoading.value = true;
  hasSearched.value = true;
  
  // 실제 구현에서는 API 호출
  await new Promise(resolve => setTimeout(resolve, 1000));
  
  isLoading.value = false;
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

.results-section { margin-top: 2rem; }
.results-title {
  font: var(--m3-title-large);
  color: var(--m3-on-surface);
  margin: 0 0 1.25rem;
}

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