<template>
  <div class="recommendations-view">
    <div class="container">
      <div class="header">
        <h1 class="title">추천 관광지</h1>
        <p class="subtitle">인기 관광지와 숨겨진 명소들을 발견하세요</p>
      </div>

      <div class="content-section">
        <!-- 필터 섹션 -->
        <div class="filter-section">
          <div class="filter-group">
            <label class="filter-label">지역</label>
            <select v-model="selectedRegion" class="filter-select">
              <option value="">전체</option>
              <option value="seoul">서울</option>
              <option value="busan">부산</option>
              <option value="jeju">제주도</option>
              <option value="gangwon">강원도</option>
            </select>
          </div>

          <div class="filter-group">
            <label class="filter-label">테마</label>
            <select v-model="selectedTheme" class="filter-select">
              <option value="">전체</option>
              <option value="culture">문화</option>
              <option value="nature">자연</option>
              <option value="food">음식</option>
              <option value="shopping">쇼핑</option>
            </select>
          </div>

          <div class="filter-group">
            <label class="filter-label">계절</label>
            <select v-model="selectedSeason" class="filter-select">
              <option value="">전체</option>
              <option value="spring">봄</option>
              <option value="summer">여름</option>
              <option value="autumn">가을</option>
              <option value="winter">겨울</option>
            </select>
          </div>
        </div>

        <!-- 추천 카테고리 -->
        <div class="categories-section">
          <h3 class="section-title">인기 카테고리</h3>
          <div class="category-grid">
            <div
              v-for="category in categories"
              :key="category.id"
              class="category-card"
              @click="selectCategory(category.id)"
            >
              <div class="category-icon">{{ category.icon }}</div>
              <h4 class="category-name">{{ category.name }}</h4>
              <p class="category-description">{{ category.description }}</p>
            </div>
          </div>
        </div>

        <!-- 추천 관광지 -->
        <div class="recommendations-section">
          <h3 class="section-title">추천 관광지</h3>
          <div class="recommendations-grid">
            <div
              v-for="spot in filteredRecommendations"
              :key="spot.id"
              class="recommendation-card"
            >
              <div class="card-image">
                <div class="image-placeholder">{{ spot.emoji }}</div>
              </div>
              <div class="card-content">
                <h4 class="spot-name">{{ spot.name }}</h4>
                <p class="spot-description">{{ spot.description }}</p>
                <div class="spot-meta">
                  <span class="spot-location">{{ spot.location }}</span>
                  <span class="spot-rating">⭐ {{ spot.rating }}</span>
                </div>
                <div class="spot-tags">
                  <span
                    v-for="tag in spot.tags"
                    :key="tag"
                    class="tag"
                  >
                    {{ tag }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

const selectedRegion = ref('');
const selectedTheme = ref('');
const selectedSeason = ref('');

const categories = ref([
  {
    id: 'culture',
    name: '문화/역사',
    icon: '🏛️',
    description: '전통 문화와 역사를 체험할 수 있는 곳'
  },
  {
    id: 'nature',
    name: '자연/풍경',
    icon: '🌿',
    description: '아름다운 자연과 풍경을 감상할 수 있는 곳'
  },
  {
    id: 'food',
    name: '맛집/음식',
    icon: '🍜',
    description: '현지의 맛있는 음식을 즐길 수 있는 곳'
  },
  {
    id: 'shopping',
    name: '쇼핑/시장',
    icon: '🛍️',
    description: '특산품과 기념품을 구매할 수 있는 곳'
  }
]);

const recommendations = ref([
  {
    id: 1,
    name: '경복궁',
    description: '조선왕조의 정궁으로 아름다운 전통 건축물을 감상할 수 있습니다.',
    location: '서울 종로구',
    rating: 4.8,
    tags: ['문화', '역사', '전통'],
    emoji: '🏛️',
    region: 'seoul',
    theme: 'culture',
    season: 'spring'
  },
  {
    id: 2,
    name: '해운대 해수욕장',
    description: '부산의 대표적인 해수욕장으로 아름다운 해변과 맑은 바다를 즐길 수 있습니다.',
    location: '부산 해운대구',
    rating: 4.6,
    tags: ['자연', '해변', '여름'],
    emoji: '🏖️',
    region: 'busan',
    theme: 'nature',
    season: 'summer'
  },
  {
    id: 3,
    name: '제주 올레길',
    description: '제주의 아름다운 자연을 걸으며 체험할 수 있는 올레길입니다.',
    location: '제주도',
    rating: 4.7,
    tags: ['자연', '트레킹', '풍경'],
    emoji: '🥾',
    region: 'jeju',
    theme: 'nature',
    season: 'autumn'
  },
  {
    id: 4,
    name: '광장시장',
    description: '서울의 대표적인 전통시장으로 다양한 먹거리와 쇼핑을 즐길 수 있습니다.',
    location: '서울 종로구',
    rating: 4.5,
    tags: ['음식', '쇼핑', '전통'],
    emoji: '🛒',
    region: 'seoul',
    theme: 'food',
    season: 'winter'
  }
]);

const filteredRecommendations = computed(() => {
  return recommendations.value.filter(spot => {
    if (selectedRegion.value && spot.region !== selectedRegion.value) return false;
    if (selectedTheme.value && spot.theme !== selectedTheme.value) return false;
    if (selectedSeason.value && spot.season !== selectedSeason.value) return false;
    return true;
  });
});

const selectCategory = (categoryId: string) => {
  selectedTheme.value = categoryId;
};
</script>

<style scoped>
/* ========== RecommendationsView — Material Design 3 ========== */
.recommendations-view {
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

.content-section {
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-xl);
  padding: clamp(1.5rem, 2.5vw, 2.5rem);
  box-shadow: var(--m3-elev-1);
}

/* Filter row */
.filter-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
  padding: 1.25rem;
  background: var(--m3-surface-container);
  border-radius: var(--m3-shape-lg);
}
.filter-group { display: flex; flex-direction: column; gap: 0.375rem; }
.filter-label {
  font: var(--m3-label-medium);
  color: var(--m3-on-surface-variant);
}
.filter-select {
  padding: 0.75rem 0.875rem;
  border: 1px solid var(--m3-outline-variant);
  border-radius: var(--m3-shape-sm);
  background: var(--m3-surface-container-lowest);
  color: var(--m3-on-surface);
  font: var(--m3-body-medium);
  transition: border-color var(--m3-motion-short), box-shadow var(--m3-motion-short);
}
.filter-select:hover { border-color: var(--m3-outline); }
.filter-select:focus {
  outline: none;
  border-color: var(--m3-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--m3-primary) 18%, transparent);
}

.section-title {
  font: var(--m3-title-large);
  color: var(--m3-on-surface);
  margin: 0 0 1.25rem;
}

.categories-section { margin-bottom: 2.5rem; }
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
}

/* M3 Filled card (tonal) */
.category-card {
  background: var(--m3-secondary-container);
  color: var(--m3-on-secondary-container);
  border-radius: var(--m3-shape-lg);
  padding: 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: box-shadow var(--m3-motion-medium), transform var(--m3-motion-medium),
    background var(--m3-motion-medium);
  position: relative;
  overflow: hidden;
}
.category-card::before {
  content: '';
  position: absolute; inset: 0;
  background: currentColor;
  opacity: 0;
  transition: opacity var(--m3-motion-short);
  pointer-events: none;
}
.category-card:hover { box-shadow: var(--m3-elev-2); transform: translateY(-2px); }
.category-card:hover::before { opacity: var(--m3-state-hover); }

.category-icon { font-size: 2.5rem; margin-bottom: 0.75rem; line-height: 1; }
.category-name {
  font: var(--m3-title-medium);
  margin: 0 0 0.25rem;
  color: inherit;
}
.category-description {
  font: var(--m3-body-medium);
  color: inherit;
  opacity: 0.88;
  margin: 0;
}

/* Recommendation cards */
.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

/* M3 Elevated card */
.recommendation-card {
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-lg);
  overflow: hidden;
  box-shadow: var(--m3-elev-1);
  transition: box-shadow var(--m3-motion-medium), transform var(--m3-motion-medium);
  cursor: pointer;
}
.recommendation-card:hover {
  box-shadow: var(--m3-elev-3);
  transform: translateY(-3px);
}

.card-image {
  height: 140px;
  background: linear-gradient(
    135deg,
    var(--m3-primary-container) 0%,
    color-mix(in srgb, var(--m3-tertiary-container) 80%, var(--m3-primary-container)) 100%
  );
  display: flex;
  align-items: center;
  justify-content: center;
}
.image-placeholder { font-size: 3.5rem; line-height: 1; }

.card-content { padding: 1.25rem; }
.spot-name {
  font: var(--m3-title-medium);
  color: var(--m3-on-surface);
  margin: 0 0 0.375rem;
}
.spot-description {
  color: var(--m3-on-surface-variant);
  font: var(--m3-body-medium);
  line-height: 1.5;
  margin: 0 0 0.875rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.spot-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}
.spot-location {
  color: var(--m3-primary);
  font: var(--m3-label-large);
}
.spot-rating {
  color: var(--m3-on-surface-variant);
  font: var(--m3-label-large);
}

.spot-tags { display: flex; flex-wrap: wrap; gap: 0.375rem; }
.tag {
  background: transparent;
  color: var(--m3-on-surface-variant);
  border: 1px solid var(--m3-outline-variant);
  padding: 0.125rem 0.625rem;
  border-radius: var(--m3-shape-sm);
  font: var(--m3-label-small);
}

@media (max-width: 768px) {
  .recommendations-view { padding: 1.25rem 0.75rem 3rem; }
  .header { margin-bottom: 1.5rem; padding: 1.5rem 1rem; }
  .content-section { padding: 1.5rem 1.25rem; }
  .filter-section { grid-template-columns: 1fr; }
  .category-grid, .recommendations-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .content-section { padding: 1.25rem 1rem; border-radius: var(--m3-shape-lg); }
  .category-card { padding: 1.25rem; }
  .card-content { padding: 1rem; }
}
</style> 