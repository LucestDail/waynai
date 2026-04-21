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
  searchStore.clearSearch();
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
/* ========== SearchInput — Material Design 3 ========== */
.search-input-container {
  max-width: 840px;
  margin: 0 auto;
  padding: clamp(1.5rem, 2.5vw, 2.5rem);
  background: var(--m3-surface-container-low);
  border-radius: var(--m3-shape-xl);
  box-shadow: var(--m3-elev-1);
  color: var(--m3-on-surface);
}

.search-header { text-align: center; margin-bottom: 1.75rem; }
.search-title {
  font: var(--m3-display-small);
  color: var(--m3-on-surface);
  margin: 0 0 0.375rem;
  letter-spacing: -0.01em;
}
.search-subtitle {
  font: var(--m3-body-large);
  color: var(--m3-on-surface-variant);
  margin: 0;
}

.search-form { display: flex; flex-direction: column; gap: 1.25rem; }

.input-group, .option-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.input-label, .option-label {
  font: var(--m3-label-large);
  color: var(--m3-on-surface-variant);
}

.search-textarea,
.option-input,
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
.search-textarea { resize: vertical; min-height: 88px; }
.search-textarea:hover,
.option-input:hover,
.option-select:hover { border-color: var(--m3-outline); }
.search-textarea:focus,
.option-input:focus,
.option-select:focus {
  outline: none;
  border-color: var(--m3-primary);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--m3-primary) 18%, transparent);
}
.search-textarea:disabled,
.option-input:disabled,
.option-select:disabled {
  background: var(--m3-surface-container);
  color: var(--m3-on-surface-variant);
  cursor: not-allowed;
}

.search-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.search-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
  flex-wrap: wrap;
  padding-top: 0.5rem;
}

.search-button,
.stop-button,
.clear-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0 1.75rem;
  min-height: 44px;
  min-width: 120px;
  border: none;
  border-radius: var(--m3-shape-full);
  font: var(--m3-label-large);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--m3-motion-short);
}
.search-button::before,
.stop-button::before,
.clear-button::before {
  content: '';
  position: absolute; inset: 0;
  background: currentColor;
  opacity: 0;
  transition: opacity var(--m3-motion-short);
  pointer-events: none;
}
.search-button:hover:not(:disabled),
.stop-button:hover:not(:disabled),
.clear-button:hover:not(:disabled) { box-shadow: var(--m3-elev-1); }
.search-button:hover:not(:disabled)::before,
.stop-button:hover:not(:disabled)::before,
.clear-button:hover:not(:disabled)::before { opacity: var(--m3-state-hover); }

/* M3 Filled primary */
.search-button { background: var(--m3-primary); color: var(--m3-on-primary); }
.search-button:disabled { opacity: 0.38; cursor: not-allowed; }
/* M3 Filled error */
.stop-button { background: var(--m3-error); color: var(--m3-on-error); }
/* M3 Outlined */
.clear-button {
  background: transparent;
  color: var(--m3-primary);
  border: 1px solid var(--m3-outline);
}

.loading-text { display: flex; align-items: center; gap: 0.5rem; }
.spinner {
  width: 16px; height: 16px;
  border: 2px solid color-mix(in srgb, currentColor 24%, transparent);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: spin 900ms linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .search-input-container { padding: 1.5rem 1.25rem; }
  .search-options { grid-template-columns: 1fr; }
  .search-actions { flex-direction: column; }
  .search-button, .stop-button, .clear-button { width: 100%; }
}
</style> 