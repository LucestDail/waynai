import { defineStore } from 'pinia';
import { reactive } from 'vue';
import streamService, { type TravelEvent } from '@/services/streamService';

export type TravelStage = 'idle' | 'analyzing' | 'searching' | 'generating' | 'completed' | 'error';

export interface SourceItem {
  title: string;
  subtitle?: string;
  url?: string;
}

export interface SourceSummary {
  source: string;
  count: number;
  items: SourceItem[];
  context?: string;
}

export interface IntentPayload {
  intent?: string;
  keyword?: string | null;
  area?: { name?: string; code?: string; sigungu?: { name?: string; code?: string } } | null;
  confidence?: number;
  reason?: string;
}

export interface TravelPlan {
  type?: string;
  destination?: string;
  duration?: string;
  days?: number;
  summary?: string;
  theme?: string;
  budget?: string;
  estimatedBudgetKrw?: number;
  transportation?: string;
  accommodation?: string;
  itinerary?: Array<{
    day?: number;
    title?: string;
    overview?: string;
    spots?: Array<{
      name?: string;
      visitTime?: string;
      durationMin?: number;
      activity?: string;
      notes?: string;
      address?: string;
      latitude?: number;
      longitude?: number;
    }>;
    activities?: string[];
    transportation?: string;
    meals?: string;
    estimatedCost?: string;
    tips?: string;
  }>;
  tips?: string[];
  weatherInfo?: string;
  warnings?: string[];
}

export interface ProgressMessage {
  stage?: string;
  type: string;
  text: string;
  at: number;
}

export interface StreamProgress {
  stage: TravelStage;
  messages: ProgressMessage[];
  intent: IntentPayload | null;
  sources: { tour: SourceSummary | null; naver: SourceSummary | null };
  model: string | null;
}

export interface StreamState {
  isStreaming: boolean;
  currentData: string;        // 누적 토큰 (AI 원문 텍스트)
  plan: TravelPlan | null;    // 구조화 파싱 성공 시
  isComplete: boolean;
  error: string | null;
  abortController: AbortController | null;
  progress: StreamProgress;
}

const createInitialProgress = (): StreamProgress => ({
  stage: 'idle',
  messages: [],
  intent: null,
  sources: { tour: null, naver: null },
  model: null,
});

const STAGE_LABEL: Record<string, TravelStage> = {
  analyzing: 'analyzing',
  searching: 'searching',
  generating: 'generating',
  completed: 'completed',
  error: 'error',
};

export const useStreamStore = defineStore('stream', () => {
  const state = reactive<StreamState>({
    isStreaming: false,
    currentData: '',
    plan: null,
    isComplete: false,
    error: null,
    abortController: null,
    progress: createInitialProgress(),
  });

  const resetProgress = () => {
    state.progress = createInitialProgress();
    state.currentData = '';
    state.plan = null;
    state.isComplete = false;
    state.error = null;
  };

  const pushMessage = (type: string, text: string | undefined, stage?: string) => {
    if (!text) return;
    state.progress.messages.push({
      stage,
      type,
      text,
      at: Date.now(),
    });
  };

  const handleEvent = (event: TravelEvent) => {
    const stage = event.stage && STAGE_LABEL[event.stage] ? STAGE_LABEL[event.stage] : undefined;
    if (stage) state.progress.stage = stage;

    switch (event.type) {
      case 'stage':
        pushMessage('stage', event.message, event.stage);
        break;
      case 'intent': {
        state.progress.intent = (event.payload as IntentPayload) ?? null;
        pushMessage('intent', event.message, event.stage);
        break;
      }
      case 'sources.tour': {
        state.progress.sources.tour = (event.payload as SourceSummary) ?? null;
        pushMessage('sources.tour', event.message, event.stage);
        break;
      }
      case 'sources.naver': {
        state.progress.sources.naver = (event.payload as SourceSummary) ?? null;
        pushMessage('sources.naver', event.message, event.stage);
        break;
      }
      case 'model': {
        const p = event.payload as { model?: string } | string | undefined;
        const model = typeof p === 'string' ? p : p?.model ?? null;
        if (model) state.progress.model = model;
        pushMessage('model', event.message, event.stage);
        break;
      }
      case 'token': {
        const payload = event.payload;
        if (typeof payload === 'string') {
          state.currentData += payload;
        }
        break;
      }
      case 'plan': {
        const payload = event.payload as TravelPlan | { fallback?: boolean; text?: string } | undefined;
        if (payload && 'fallback' in payload && payload.fallback) {
          // 텍스트 폴백이면 currentData 만 유지
          if (payload.text && !state.currentData) state.currentData = payload.text;
        } else if (payload) {
          state.plan = payload as TravelPlan;
        }
        if (event.message) pushMessage('plan', event.message, event.stage);
        break;
      }
      case 'done': {
        state.progress.stage = 'completed';
        state.isComplete = true;
        pushMessage('done', event.message ?? '여행 계획 생성 완료', 'completed');
        break;
      }
      case 'error': {
        state.progress.stage = 'error';
        state.error = event.message ?? '알 수 없는 오류';
        pushMessage('error', event.message, 'error');
        break;
      }
      default:
        break;
    }
  };

  const startTravelPlanStream = async (query: string) => {
    if (state.isStreaming) {
      console.warn('[stream] 이미 진행 중입니다.');
      return;
    }
    if (state.abortController) {
      state.abortController.abort();
      state.abortController = null;
    }

    resetProgress();
    state.isStreaming = true;
    state.progress.stage = 'analyzing';

    try {
      const controller = await streamService.generateTravelPlanTyped(query, {
        onEvent: handleEvent,
        onComplete: () => {
          state.isStreaming = false;
          if (!state.isComplete && !state.error) {
            state.isComplete = true;
            state.progress.stage = 'completed';
          }
          state.abortController = null;
          window.dispatchEvent(new CustomEvent('streamComplete'));
        },
        onError: (err) => {
          state.isStreaming = false;
          state.error = err.message || '스트림 오류';
          state.progress.stage = 'error';
          state.abortController = null;
          window.dispatchEvent(new CustomEvent('streamError'));
        },
      });
      state.abortController = controller;
    } catch (error) {
      console.error('[stream] 시작 실패:', error);
      state.isStreaming = false;
      state.error = error instanceof Error ? error.message : '알 수 없는 오류';
      state.progress.stage = 'error';
    }
  };

  const startChatStream = async (message: string, sessionId: string = 'default-session') => {
    if (state.abortController) {
      state.abortController.abort();
    }
    resetProgress();
    state.isStreaming = true;

    try {
      const controller = await streamService.sendChatMessage(
        message,
        sessionId,
        (chunk: string) => {
          state.currentData += chunk;
        },
        () => {
          state.isStreaming = false;
          state.isComplete = true;
          state.abortController = null;
        },
        (error: Error) => {
          state.isStreaming = false;
          state.error = error.message;
          state.abortController = null;
        }
      );
      state.abortController = controller;
    } catch (error) {
      state.isStreaming = false;
      state.error = error instanceof Error ? error.message : '알 수 없는 오류';
    }
  };

  const stopStream = () => {
    if (state.abortController) {
      state.abortController.abort();
      state.abortController = null;
    }
    state.isStreaming = false;
  };

  const clearStream = () => {
    stopStream();
    resetProgress();
  };

  const setData = (data: string) => {
    state.currentData = cleanDataPrefix(data);
    state.isStreaming = false;
    state.isComplete = true;
    state.error = null;
    state.progress.stage = 'completed';
  };

  const setError = (error: string) => {
    state.error = error;
    state.isStreaming = false;
    state.isComplete = false;
    state.progress.stage = 'error';
  };

  const setStreaming = (streaming: boolean) => {
    state.isStreaming = streaming;
    if (streaming) {
      resetProgress();
      state.progress.stage = 'analyzing';
    }
  };

  const cleanDataPrefix = (text: string): string => {
    if (!text) return text;
    return text
      .split('\n')
      .map((line) => line.replace(/^data:\s*/, ''))
      .join('\n');
  };

  const formatMarkdown = (text: string): string => {
    if (!text) return '';
    const cleaned = cleanDataPrefix(text.trim());

    let html = cleaned
      .replace(/^#{1}\s+(.*$)/gim, '<h1 class="markdown-h1">$1</h1>')
      .replace(/^#{2}\s+(.*$)/gim, '<h2 class="markdown-h2">$1</h2>')
      .replace(/^#{3}\s+(.*$)/gim, '<h3 class="markdown-h3">$1</h3>')
      .replace(/^#{4}\s+(.*$)/gim, '<h4 class="markdown-h4">$1</h4>')
      .replace(/^#{5}\s+(.*$)/gim, '<h5 class="markdown-h5">$1</h5>')
      .replace(/^#{6}\s+(.*$)/gim, '<h6 class="markdown-h6">$1</h6>')
      .replace(/\*\*(.*?)\*\*/g, '<strong class="markdown-bold">$1</strong>')
      .replace(/\*(.*?)\*/g, '<em class="markdown-italic">$1</em>')
      .replace(/`(.*?)`/g, '<code class="markdown-code">$1</code>')
      .replace(/~~(.*?)~~/g, '<del class="markdown-strikethrough">$1</del>')
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="markdown-link" target="_blank" rel="noopener noreferrer">$1</a>')
      .replace(/^---$/gim, '<hr class="markdown-hr">')
      .replace(/^\*\*\*$/gim, '<hr class="markdown-hr">')
      .replace(/^>\s*(.*$)/gim, '<blockquote class="markdown-blockquote">$1</blockquote>');

    html = html
      .replace(/^[\s]*[-*+]\s+(.*$)/gim, '<li class="markdown-list-item">$1</li>')
      .replace(/^[\s]*(\d+)[.)]\s+(.*$)/gim, '<li class="markdown-list-item markdown-ordered">$1. $2</li>');

    html = html
      .replace(/\n\n+/g, '</p><p class="markdown-paragraph">')
      .replace(/\n/g, '<br class="markdown-linebreak">');

    if (!html.startsWith('<')) {
      html = '<p class="markdown-paragraph">' + html + '</p>';
    }

    html = html.replace(/(<li class="markdown-list-item">(?!.*markdown-ordered).*?<\/li>)/g, '<ul class="markdown-list">$1</ul>');
    html = html.replace(/<\/ul><ul class="markdown-list">/g, '');
    html = html.replace(/(<li class="markdown-list-item markdown-ordered">.*?<\/li>)/g, '<ol class="markdown-ordered-list">$1</ol>');
    html = html.replace(/<\/ol><ol class="markdown-ordered-list">/g, '');
    html = html.replace(/<\/blockquote><blockquote class="markdown-blockquote">/g, '<br>');

    return html;
  };

  return {
    state,
    startTravelPlanStream,
    startChatStream,
    stopStream,
    clearStream,
    formatMarkdown,
    setData,
    setError,
    setStreaming,
  };
});
