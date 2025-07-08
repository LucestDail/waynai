import axios from 'axios';
import type { SearchRequest, SearchResponse } from '@/types/search';

const API_BASE_URL = 'http://localhost:8080';

export class SearchService {
  private static instance: SearchService;
  private eventSource: EventSource | null = null;

  private constructor() {}

  public static getInstance(): SearchService {
    if (!SearchService.instance) {
      SearchService.instance = new SearchService();
    }
    return SearchService.instance;
  }

  public async searchStream(
    request: SearchRequest,
    onMessage: (response: SearchResponse) => void,
    onError: (error: Event) => void,
    onComplete: () => void
  ): Promise<void> {
    try {
      // 기존 연결이 있다면 닫기
      if (this.eventSource) {
        this.eventSource.close();
      }

      // POST 요청으로 스트림 시작
      const response = await axios.post(
        `${API_BASE_URL}/api/search/stream`,
        request,
        {
          headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer valid-token'
          },
          responseType: 'stream'
        }
      );

      // EventSource를 사용하여 SSE 연결
      const url = `${API_BASE_URL}/api/search/stream`;
      this.eventSource = new EventSource(url);

      this.eventSource.onmessage = (event) => {
        try {
          const data: SearchResponse = JSON.parse(event.data);
          onMessage(data);
        } catch (error) {
          console.error('Failed to parse SSE message:', error);
        }
      };

      this.eventSource.onerror = (error) => {
        console.error('SSE error:', error);
        onError(error);
        this.closeConnection();
      };

      this.eventSource.onopen = () => {
        console.log('SSE connection opened');
      };

    } catch (error) {
      console.error('Failed to start search stream:', error);
      onError(error as Event);
    }
  }

  public closeConnection(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
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