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
.tourist-info-view {
  min-height: 100vh;
  background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
  padding: 2rem 1rem;
  transition: background 0.3s ease;
}

/* 다크모드에서 관광 정보 배경 */
.dark .tourist-info-view {
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

.search-section {
  background: white;
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  transition: background 0.3s ease;
}

/* 다크모드에서 검색 섹션 */
.dark .search-section {
  background: #1e293b;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.input-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 1rem;
}

.search-input {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.3s ease;
}

.search-input:focus {
  outline: none;
  border-color: #3498db;
}

.search-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.option-label {
  font-weight: 600;
  color: #2c3e50;
  font-size: 0.9rem;
}

.option-select {
  padding: 0.75rem;
  border: 2px solid #e1e8ed;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.3s ease;
}

.option-select:focus {
  outline: none;
  border-color: #3498db;
}

.search-actions {
  display: flex;
  justify-content: center;
}

.search-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 1rem 2rem;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease;
}

/* 다크모드에서 검색 버튼 */
.dark .search-button {
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
}

.search-button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.search-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-container {
  text-align: center;
  padding: 3rem 1rem;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 4px solid #f3f4f6;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-text {
  color: #6b7280;
  font-size: 1.1rem;
}

.results-section {
  margin-top: 2rem;
}

.results-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 1.5rem;
}

.tourist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
}

.tourist-card {
  background: white;
  border: 1px solid #e1e8ed;
  border-radius: 12px;
  padding: 1.5rem;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.3s ease;
}

/* 다크모드에서 관광지 카드 */
.dark .tourist-card {
  background: #1e293b;
  border: 1px solid #475569;
}

.tourist-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.spot-name {
  font-size: 1.2rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
  transition: color 0.3s ease;
}

/* 다크모드에서 관광지 이름 */
.dark .spot-name {
  color: #f8fafc;
}

.spot-category {
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.8rem;
  font-weight: 500;
}

.card-content {
  color: #6b7280;
  transition: color 0.3s ease;
}

/* 다크모드에서 카드 콘텐츠 */
.dark .card-content {
  color: #cbd5e1;
}

.spot-description {
  margin-bottom: 1rem;
  line-height: 1.6;
}

.spot-details {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.detail-item {
  font-size: 0.9rem;
}

.empty-results {
  text-align: center;
  padding: 3rem 1rem;
  color: #6b7280;
  font-size: 1.1rem;
}

/* 모바일 대응 */
@media (max-width: 768px) {
  .tourist-info-view {
    padding: 1rem 0.5rem;
  }
  
  .title {
    font-size: 2rem;
  }
  
  .subtitle {
    font-size: 1rem;
  }
  
  .search-section {
    padding: 1.5rem;
    margin: 0 0.5rem;
  }
  
  .search-options {
    grid-template-columns: 1fr;
  }
  
  .tourist-grid {
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
  
  .search-section {
    padding: 1rem;
  }
  
  .tourist-card {
    padding: 1rem;
  }
}
</style> 