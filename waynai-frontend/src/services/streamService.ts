/**
 * 스트림 서비스
 * 백엔드의 SSE API 와 통신. 타입드 이벤트 (event:/data:) 파서를 포함.
 */

export interface TravelEvent {
  type: string;         // stage | intent | sources.tour | sources.naver | model | token | plan | done | error
  stage?: string;       // analyzing | searching | generating | completed | error
  message?: string;
  payload?: unknown;
}

export interface TravelPlanStreamHandlers {
  onEvent: (event: TravelEvent) => void;
  onComplete: () => void;
  onError: (error: Error) => void;
}

const DEFAULT_API_BASE = 'http://localhost:8080';

export class StreamService {
  private static instance: StreamService;
  private readonly API_BASE_URL = import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE;

  private constructor() {}

  public static getInstance(): StreamService {
    if (!StreamService.instance) {
      StreamService.instance = new StreamService();
    }
    return StreamService.instance;
  }

  /**
   * 타입드 여행 계획 스트림 (신규 엔드포인트 /api/travel/plan/stream 구독).
   * 서버가 event:/data: 라인으로 JSON payload 를 보낸다.
   */
  public async generateTravelPlanTyped(
    query: string,
    handlers: TravelPlanStreamHandlers
  ): Promise<AbortController> {
    const controller = new AbortController();

    const url = `${this.API_BASE_URL}/api/travel/plan/stream?query=${encodeURIComponent(query)}`;

    (async () => {
      try {
        const response = await fetch(url, {
          method: 'GET',
          headers: {
            Accept: 'text/event-stream',
            'Cache-Control': 'no-cache',
          },
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status} ${response.statusText}`);
        }
        if (!response.body) {
          throw new Error('응답 본문이 없습니다.');
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        // 현재 파싱 중인 이벤트 정보
        let currentEventName = 'message';
        const currentDataLines: string[] = [];

        const dispatch = () => {
          if (currentDataLines.length === 0) {
            currentEventName = 'message';
            return;
          }
          const dataRaw = currentDataLines.join('\n');
          currentDataLines.length = 0;
          const typeFromEvent = currentEventName;
          currentEventName = 'message';

          let event: TravelEvent;
          try {
            const parsed = JSON.parse(dataRaw);
            // 서버 TravelEvent DTO 와 동일 구조.
            event = {
              type: parsed.type ?? typeFromEvent,
              stage: parsed.stage,
              message: parsed.message,
              payload: parsed.payload,
            };
          } catch {
            // JSON 이 아닌 경우(토큰 등) raw 텍스트로 처리.
            event = {
              type: typeFromEvent,
              payload: dataRaw,
            };
          }
          try {
            handlers.onEvent(event);
          } catch (err) {
            console.error('[stream] onEvent 핸들러 오류 (무시):', err);
          }
        };

        const processLine = (line: string) => {
          if (line === '') {
            dispatch();
            return;
          }
          if (line.startsWith(':')) {
            // 주석/heartbeat
            return;
          }
          const colon = line.indexOf(':');
          const field = colon === -1 ? line : line.slice(0, colon);
          let value = colon === -1 ? '' : line.slice(colon + 1);
          if (value.startsWith(' ')) value = value.slice(1);

          switch (field) {
            case 'event':
              currentEventName = value || 'message';
              break;
            case 'data':
              currentDataLines.push(value);
              break;
            case 'id':
            case 'retry':
              break;
            default:
              break;
          }
        };

        while (true) {
          const { done, value } = await reader.read();
          if (done) {
            if (buffer.length > 0) {
              // 남은 버퍼 flush
              const lines = buffer.split(/\r?\n/);
              for (const l of lines) processLine(l);
              dispatch();
            }
            handlers.onComplete();
            break;
          }

          buffer += decoder.decode(value, { stream: true });
          let newlineIdx;
          while ((newlineIdx = buffer.search(/\r?\n/)) !== -1) {
            const raw = buffer.slice(0, newlineIdx);
            const skip = buffer[newlineIdx] === '\r' && buffer[newlineIdx + 1] === '\n' ? 2 : 1;
            buffer = buffer.slice(newlineIdx + skip);
            processLine(raw);
          }
        }
      } catch (error) {
        if ((error as Error)?.name === 'AbortError') {
          return;
        }
        console.error('[stream] 오류:', error);
        handlers.onError(error as Error);
      }
    })();

    return controller;
  }

  /**
   * 레거시 /api/travel/plan 텍스트 청크 스트림. 호환성 유지를 위해 유지.
   * 새 코드는 generateTravelPlanTyped 를 사용하세요.
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
            Accept: 'text/event-stream',
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
      let buffer = '';

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) {
            onComplete();
            break;
          }
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const cleanData = line.replace(/^data:\s*/, '');
              if (cleanData.trim()) onData(cleanData);
            }
          }
        }
      } finally {
        reader.releaseLock();
      }
    } catch (error) {
      if ((error as Error)?.name === 'AbortError') return controller;
      onError(error as Error);
    }

    return controller;
  }

  /**
   * 채팅 메시지 스트림 (레거시, 단순 텍스트 청크).
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
            Accept: 'text/plain',
            'Cache-Control': 'no-cache',
          },
          signal: controller.signal,
        }
      );

      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      if (!response.body) throw new Error('No response body');

      const reader = response.body.getReader();
      const decoder = new TextDecoder();

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) {
            onComplete();
            break;
          }
          onData(decoder.decode(value, { stream: true }));
        }
      } finally {
        reader.releaseLock();
      }
    } catch (error) {
      if ((error as Error)?.name === 'AbortError') return controller;
      onError(error as Error);
    }

    return controller;
  }
}

export default StreamService.getInstance();
