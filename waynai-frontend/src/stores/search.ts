import { defineStore } from 'pinia';
import { ref, reactive } from 'vue';
import type { SearchRequest, SearchResponse, SearchState } from '@/types/search';
import searchService from '@/services/searchService';

export const useSearchStore = defineStore('search', () => {
  const state = reactive<SearchState>({
    isSearching: false,
    currentStatus: '',
    currentStep: '',
    progress: [],
    result: null,
    error: null
  });

  const isLoading = ref(false);

  const startSearch = async (request: SearchRequest) => {
    try {
      isLoading.value = true;
      state.isSearching = true;
      state.currentStatus = '';
      state.currentStep = '';
      state.progress = [];
      state.result = null;
      state.error = null;

      await searchService.searchStream(
        request,
        (response: SearchResponse) => {
          state.currentStatus = response.status;
          state.currentStep = response.step;
          state.progress = response.progress;
          
          if (response.data) {
            state.result = response.data;
          }
          
          if (response.status === 'completed') {
            state.isSearching = false;
            isLoading.value = false;
          }
        },
        (error: Event) => {
          state.error = '검색 중 오류가 발생했습니다.';
          state.isSearching = false;
          isLoading.value = false;
          console.error('Search error:', error);
        },
        () => {
          state.isSearching = false;
          isLoading.value = false;
        }
      );
    } catch (error) {
      state.error = '검색을 시작할 수 없습니다.';
      state.isSearching = false;
      isLoading.value = false;
      console.error('Failed to start search:', error);
    }
  };

  const stopSearch = () => {
    searchService.closeConnection();
    state.isSearching = false;
    isLoading.value = false;
  };

  const clearSearch = () => {
    state.currentStatus = '';
    state.currentStep = '';
    state.progress = [];
    state.result = null;
    state.error = null;
  };

  const checkHealth = async (): Promise<boolean> => {
    return await searchService.healthCheck();
  };

  return {
    state,
    isLoading,
    startSearch,
    stopSearch,
    clearSearch,
    checkHealth
  };
}); 