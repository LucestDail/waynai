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
.recommendations-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 2rem 1rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 추천 배경 */
.dark .recommendations-view {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  text-align: center;
  margin-bottom: 3rem;
  color: white;
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #ffffff 0%, #e0e7ff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
  color: #e0e7ff;
}

.content-section {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  transition: background 0.3s ease;
}

/* 다크모드에서 콘텐츠 섹션 */
.dark .content-section {
  background: #1e293b;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
}

.filter-section {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  min-width: 150px;
}

.filter-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 0.9rem;
}

.filter-select {
  padding: 0.75rem;
  border: 2px solid #e1e8ed;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.3s ease, background 0.3s ease, color 0.3s ease;
}

.filter-select:focus {
  outline: none;
  border-color: #3498db;
}

/* 다크모드에서 필터 선택 */
.dark .filter-select {
  background: #1e293b;
  border-color: #475569;
  color: #f1f5f9;
}

.dark .filter-select:focus {
  border-color: #60a5fa;
}

.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 1.5rem;
}

.categories-section {
  margin-bottom: 3rem;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.category-card {
  background: #f8fafc;
  border: 2px solid #e1e8ed;
  border-radius: 12px;
  padding: 1.5rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.category-card:hover {
  border-color: #667eea;
  transform: translateY(-4px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

/* 다크모드에서 카테고리 카드 */
.dark .category-card {
  background: #1e293b;
  border-color: #475569;
}

.dark .category-card:hover {
  border-color: #60a5fa;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.3);
}

.category-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.category-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
  transition: color 0.3s ease;
}

.category-description {
  color: #6b7280;
  font-size: 0.9rem;
  line-height: 1.5;
  transition: color 0.3s ease;
}

/* 다크모드에서 카테고리 정보 */
.dark .category-name {
  color: #f8fafc;
}

.dark .category-description {
  color: #cbd5e1;
}

.recommendations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.recommendation-card {
  background: white;
  border: 1px solid #e1e8ed;
  border-radius: 12px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.3s ease;
}

/* 다크모드에서 추천 카드 */
.dark .recommendation-card {
  background: #1e293b;
  border: 1px solid #475569;
}

.recommendation-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

.card-image {
  height: 150px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.3s ease;
}

/* 다크모드에서 카드 이미지 */
.dark .card-image {
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
}

.image-placeholder {
  font-size: 4rem;
  color: white;
}

.card-content {
  padding: 1.5rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 카드 콘텐츠 */
.dark .card-content {
  background: #1e293b;
}

.spot-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
  transition: color 0.3s ease;
}

.spot-description {
  color: #6b7280;
  line-height: 1.6;
  margin-bottom: 1rem;
  transition: color 0.3s ease;
}

/* 다크모드에서 관광지 정보 */
.dark .spot-name {
  color: #f8fafc;
}

.dark .spot-description {
  color: #cbd5e1;
}

.spot-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.spot-location {
  color: #667eea;
  font-weight: 500;
  font-size: 0.9rem;
}

.spot-rating {
  color: #f59e0b;
  font-weight: 500;
  font-size: 0.9rem;
}

.spot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.tag {
  background: #f3f4f6;
  color: #6b7280;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 500;
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .recommendations-view {
    padding: 1rem 0.5rem;
  }
  
  .title {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .content-section {
    padding: 1.5rem;
    margin: 0 0.5rem;
  }
  
  .filter-section {
    flex-direction: column;
  }
  
  .filter-group {
    min-width: auto;
  }
  
  .category-grid {
    grid-template-columns: 1fr;
  }
  
  .recommendations-grid {
    grid-template-columns: 1fr;
  }
  
  .header {
    margin-bottom: 2rem;
  }
}

@media (max-width: 480px) {
  .title {
    font-size: 1.8rem;
  }
  
  .content-section {
    padding: 1rem;
  }
  
  .category-card {
    padding: 1rem;
  }
  
  .recommendation-card .card-content {
    padding: 1rem;
  }
}
</style> 