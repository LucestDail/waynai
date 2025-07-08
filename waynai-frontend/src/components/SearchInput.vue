<template>
  <div class="search-input-container">
    <div class="search-header">
              <h1 class="search-title">WaynAI 여행 검색</h1>
      <p class="search-subtitle">키워드나 여행 계획을 입력하세요</p>
    </div>

    <div class="search-form">
      <div class="input-group">
        <label for="searchQuery" class="input-label">검색어</label>
        <textarea
          id="searchQuery"
          v-model="searchQuery"
          class="search-textarea"
          placeholder="예시: '경복궁' 또는 '서울에서 3일간 문화 여행하고 싶어요'"
          :disabled="state.isSearching"
          rows="3"
        ></textarea>
      </div>

      <div class="search-options">
        <div class="option-group">
          <label for="destination" class="option-label">목적지</label>
          <input
            id="destination"
            v-model="destination"
            type="text"
            class="option-input"
            placeholder="서울, 부산, 제주 등"
            :disabled="state.isSearching"
          />
        </div>

        <div class="option-group">
          <label for="theme" class="option-label">테마</label>
          <select
            id="theme"
            v-model="theme"
            class="option-select"
            :disabled="state.isSearching"
          >
            <option value="">선택하세요</option>
            <option value="문화">문화</option>
            <option value="자연">자연</option>
            <option value="쇼핑">쇼핑</option>
            <option value="음식">음식</option>
            <option value="레저">레저</option>
          </select>
        </div>

        <div class="option-group">
          <label for="days" class="option-label">여행 일수</label>
          <input
            id="days"
            v-model.number="days"
            type="number"
            class="option-input"
            placeholder="1-7일"
            min="1"
            max="7"
            :disabled="state.isSearching"
          />
        </div>
      </div>

      <div class="search-actions">
        <button
          @click="handleSearch"
          class="search-button"
          :disabled="!searchQuery.trim() || state.isSearching"
        >
          <span v-if="!state.isSearching">검색 시작</span>
          <span v-else class="loading-text">
            <span class="spinner"></span>
            검색 중...
          </span>
        </button>

        <button
          v-if="state.isSearching"
          @click="handleStop"
          class="stop-button"
        >
          검색 중단
        </button>

        <button
          v-if="state.result || state.error"
          @click="handleClear"
          class="clear-button"
        >
          초기화
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useSearchStore } from '@/stores/search';
import type { SearchRequest } from '@/types/search';

const searchStore = useSearchStore();
const { state } = searchStore;

const searchQuery = ref('');
const destination = ref('');
const theme = ref('');
const days = ref<number | null>(null);

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return;

  const request: SearchRequest = {
    query: searchQuery.value.trim(),
    destination: destination.value || undefined,
    theme: theme.value || undefined,
    days: days.value || undefined
  };

  await searchStore.startSearch(request);
};

const handleStop = () => {
  searchStore.stopSearch();
};

const handleClear = () => {
  searchQuery.value = '';
  destination.value = '';
  theme.value = '';
  days.value = null;
  searchStore.clearSearch();
};
</script>

<style scoped>
.search-input-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 2rem;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.search-header {
  text-align: center;
  margin-bottom: 2rem;
}

.search-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 0.5rem;
}

.search-subtitle {
  font-size: 1.1rem;
  color: #7f8c8d;
  margin: 0;
}

.search-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
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

.search-textarea {
  width: 100%;
  padding: 1rem;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 1rem;
  resize: vertical;
  transition: border-color 0.3s ease;
}

.search-textarea:focus {
  outline: none;
  border-color: #3498db;
}

.search-textarea:disabled {
  background-color: #f8f9fa;
  cursor: not-allowed;
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

.option-input,
.option-select {
  padding: 0.75rem;
  border: 2px solid #e1e8ed;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.3s ease;
}

.option-input:focus,
.option-select:focus {
  outline: none;
  border-color: #3498db;
}

.option-input:disabled,
.option-select:disabled {
  background-color: #f8f9fa;
  cursor: not-allowed;
}

.search-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.search-button,
.stop-button,
.clear-button {
  padding: 0.75rem 2rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 120px;
}

.search-button {
  background-color: #3498db;
  color: white;
}

.search-button:hover:not(:disabled) {
  background-color: #2980b9;
  transform: translateY(-2px);
}

.search-button:disabled {
  background-color: #bdc3c7;
  cursor: not-allowed;
  transform: none;
}

.stop-button {
  background-color: #e74c3c;
  color: white;
}

.stop-button:hover {
  background-color: #c0392b;
  transform: translateY(-2px);
}

.clear-button {
  background-color: #95a5a6;
  color: white;
}

.clear-button:hover {
  background-color: #7f8c8d;
  transform: translateY(-2px);
}

.loading-text {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .search-input-container {
    padding: 1rem;
    margin: 1rem;
  }

  .search-title {
    font-size: 2rem;
  }

  .search-options {
    grid-template-columns: 1fr;
  }

  .search-actions {
    flex-direction: column;
  }

  .search-button,
  .stop-button,
  .clear-button {
    width: 100%;
  }
}
</style> 