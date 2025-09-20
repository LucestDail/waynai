/**
 * 스트림 서비스
 * 백엔드의 스트림 API와 통신
 */
export class StreamService {
  private static instance: StreamService;
  private readonly API_BASE_URL = 'http://localhost:8080';

  private constructor() {}

  public static getInstance(): StreamService {
    if (!StreamService.instance) {
      StreamService.instance = new StreamService();
    }
    return StreamService.instance;
  }

  /**
   * 여행 계획 생성 스트림
   * @param query 사용자 입력
   * @param onData 데이터 수신 콜백
   * @param onComplete 완료 콜백
   * @param onError 오류 콜백
   * @returns AbortController
   */
  public async generateTravelPlan(
    query: string,
    onData: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<AbortController> {
    const controller = new AbortController();
    
    try {
      const response = await fetch(
        `${this.API_BASE_URL}/api/travel/plan?query=${encodeURIComponent(query)}`,
        {
          method: 'GET',
          headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
          },
          signal: controller.signal,
        }
      );

      console.log('스트림 응답 받음:', {
        status: response.status,
        statusText: response.statusText,
        contentType: response.headers.get('content-type'),
        ok: response.ok
      });

      if (!response.ok) {
        console.error('HTTP 응답 오류:', response.status, response.statusText);
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      if (!response.body) {
        console.error('응답 body가 없습니다');
        throw new Error('No response body');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      try {
        while (true) {
          const { done, value } = await reader.read();
          
          if (done) {
            onComplete();
            break;
          }

          buffer += decoder.decode(value, { stream: true });
          
          // Server-Sent Events 파싱
          const lines = buffer.split('\n');
          buffer = lines.pop() || ''; // 마지막 불완전한 라인은 버퍼에 보관
          
          for (const line of lines) {
            console.log('파싱 중인 라인:', line);
            if (line.startsWith('data: ')) {
              // 모든 data: 접두사를 제거 (정규식 사용)
              const cleanData = line.replace(/^data:\s*/, '').replace(/^data:\s*/g, '');
              console.log('정리된 데이터:', cleanData);
              
              if (cleanData.trim()) {
                console.log('최종 전송 데이터:', cleanData);
                onData(cleanData);
              }
            } else if (line.trim() === 'data:') {
              // 빈 data: 라인은 무시
              console.log('빈 data: 라인 무시');
              continue;
            }
          }
        }
      } finally {
        reader.releaseLock();
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log('Stream aborted');
      } else {
        console.error('Stream error:', error);
        onError(error as Error);
      }
    }

    return controller;
  }

  /**
   * 채팅 메시지 스트림
   * @param message 사용자 메시지
   * @param sessionId 세션 ID
   * @param onData 데이터 수신 콜백
   * @param onComplete 완료 콜백
   * @param onError 오류 콜백
   * @returns AbortController
   */
  public async sendChatMessage(
    message: string,
    sessionId: string,
    onData: (chunk: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ): Promise<AbortController> {
    const controller = new AbortController();
    
    try {
      const response = await fetch(
        `${this.API_BASE_URL}/api/chat/simple?message=${encodeURIComponent(message)}&sessionId=${encodeURIComponent(sessionId)}`,
        {
          method: 'GET',
          headers: {
            'Accept': 'text/plain',
            'Cache-Control': 'no-cache',
          },
          signal: controller.signal,
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      if (!response.body) {
        throw new Error('No response body');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      try {
        while (true) {
          const { done, value } = await reader.read();
          
          if (done) {
            onComplete();
            break;
          }

          const chunk = decoder.decode(value, { stream: true });
          onData(chunk);
        }
      } finally {
        reader.releaseLock();
      }
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') {
        console.log('Chat stream aborted');
      } else {
        console.error('Chat stream error:', error);
        onError(error as Error);
      }
    }

    return controller;
  }
}

export default StreamService.getInstance();
