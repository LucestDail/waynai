import axios from 'axios';
import type { SearchRequest, SearchResponse } from '@/types/search';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://3.35.206.187:8080';

export class SearchService {
  private static instance: SearchService;

  private constructor() {}

  public static getInstance(): SearchService {
    if (!SearchService.instance) {
      SearchService.instance = new SearchService();
    }
    return SearchService.instance;
  }

  public async search(
    request: SearchRequest
  ): Promise<SearchResponse> {
    try {
      console.log('Sending search request:', request);
      
      const response = await axios.post(`${API_BASE_URL}/api/search/search`, request, {
        headers: {
          'Content-Type': 'application/json'
        }
      });

      console.log('Search response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Search failed:', error);
      throw error;
    }
  }

  public async healthCheck(): Promise<boolean> {
    try {
      const response = await axios.get(`${API_BASE_URL}/api/search/health`);
      return response.status === 200;
    } catch (error) {
      console.error('Health check failed:', error);
      return false;
    }
  }
}

export default SearchService.getInstance(); 