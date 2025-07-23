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

      console.log('Starting search with request:', request);

      const response = await searchService.search(request);
      console.log('Received search response:', response);
      
      // 상태 업데이트
      if (response.status) {
        state.currentStatus = response.status;
      }
      
      if (response.step) {
        state.currentStep = response.step;
      }
      
      if (response.progress && Array.isArray(response.progress)) {
        state.progress = [...response.progress];
      }
      
      // 결과 데이터 처리
      if (response.data !== undefined) {
        console.log('Received result data:', response.data);
        state.result = response.data;
      }
      
      // 에러 상태 처리
      if (response.status === 'error') {
        console.error('Search completed with error:', response.data);
        state.error = response.data || '검색 중 오류가 발생했습니다.';
      }
      
      state.isSearching = false;
      isLoading.value = false;
      
    } catch (error) {
      console.error('Search failed:', error);
      state.error = '검색 중 오류가 발생했습니다.';
      state.isSearching = false;
      isLoading.value = false;
    }
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
    clearSearch,
    checkHealth
  };
}); 