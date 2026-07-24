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
  international?: boolean;
  destination?: string;
  days?: number;
  origin?: string;
  departDate?: string;
  returnDate?: string;
  style?: string;
  budgetLevel?: string;
  companions?: string;
  confidence?: number;
  reason?: string;
}

export interface Accommodation {
  name?: string;
  area?: string;
  type?: string;
  pricePerNightKrw?: number;
  bookingUrl?: string;
}

export interface Meal {
  type?: string;
  name?: string;
  location?: string;
  menu?: string;
  priceKrw?: number;
}

export interface CostItem {
  label?: string;
  krw?: number;
}

export interface FlightOffer {
  origin?: string;
  destination?: string;
  airline?: string;
  flightNumber?: number;
  transfers?: number;
  price?: number;
  currency?: string;
  departureAt?: string;
  returnAt?: string;
  legMinutes?: number;
  roundTrip?: boolean;
  bookingUrl?: string;
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
  accommodation?: Accommodation;
  international?: boolean;
  flights?: FlightOffer[];
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
    meals?: Meal[];
    accommodation?: string;
    weather?: string;
    estimatedCost?: string;
    costItems?: CostItem[];
    tips?: string;
  }>;
  tips?: string[];
  weatherInfo?: string;
  localInfo?: string;
  packingList?: string[];
  costBreakdown?: {
    flightsKrw?: number;
    accommodationKrw?: number;
    foodKrw?: number;
    transportKrw?: number;
    activitiesKrw?: number;
    etcKrw?: number;
  };
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
  sources: { tour: SourceSummary | null; naver: SourceSummary | null; web: SourceSummary | null };
  model: string | null;
}

export interface StreamState {
  isStreaming: boolean;
  currentData: string;        // 누적 토큰 (AI 원문 텍스트)
  plan: TravelPlan | null;    // 구조화 파싱 성공 시
  partialPlan: Partial<TravelPlan> | null; // 생성 중 부분 파싱 결과 (실시간 미리보기)
  flights: FlightOffer[];     // sources.flight 이벤트로 수신한 항공권 (plan 실패해도 표시)
  hotels: Accommodation[];    // sources.hotel 이벤트로 수신한 숙소
  isComplete: boolean;
  error: string | null;
  abortController: AbortController | null;
  progress: StreamProgress;
}

const createInitialProgress = (): StreamProgress => ({
  stage: 'idle',
  messages: [],
  intent: null,
  sources: { tour: null, naver: null, web: null },
  model: null,
});

const STAGE_LABEL: Record<string, TravelStage> = {
  analyzing: 'analyzing',
  searching: 'searching',
  generating: 'generating',
  completed: 'completed',
  error: 'error',
};

/** 스트리밍 중 완성된 최상위 {…} 객체들을 추출 (문자열/이스케이프 인식). ] 만나면 종료. */
function extractObjects(s: string): Array<Record<string, unknown>> {
  const objs: Array<Record<string, unknown>> = [];
  let depth = 0, inStr = false, esc = false, startI = -1;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (inStr) {
      if (esc) esc = false;
      else if (c === '\\') esc = true;
      else if (c === '"') inStr = false;
      continue;
    }
    if (c === '"') inStr = true;
    else if (c === '{') { if (depth === 0) startI = i; depth++; }
    else if (c === '}') {
      depth--;
      if (depth === 0 && startI >= 0) {
        try { objs.push(JSON.parse(s.slice(startI, i + 1))); } catch { /* 미완성 스킵 */ }
        startI = -1;
      }
    } else if (c === ']' && depth === 0) break;
  }
  return objs;
}

/** 완성된 날짜들 + 현재 작성 중인(미완성) 날짜의 부분 내용을 함께 추출. */
function extractItineraryDays(arrStr: string): Array<Record<string, unknown>> {
  const days: Array<Record<string, unknown>> = [];
  let depth = 0, inStr = false, esc = false, startI = -1, lastEnd = 0, closed = false;
  for (let i = 0; i < arrStr.length; i++) {
    const c = arrStr[i];
    if (inStr) {
      if (esc) esc = false;
      else if (c === '\\') esc = true;
      else if (c === '"') inStr = false;
      continue;
    }
    if (c === '"') inStr = true;
    else if (c === '{') { if (depth === 0) startI = i; depth++; }
    else if (c === '}') {
      depth--;
      if (depth === 0 && startI >= 0) {
        try { days.push(JSON.parse(arrStr.slice(startI, i + 1))); } catch { /* skip */ }
        lastEnd = i + 1; startI = -1;
      }
    } else if (c === ']' && depth === 0) { closed = true; break; }
  }
  // 배열이 아직 안 닫혔으면 = 마지막에 작성 중인 날짜가 있음 → title/spots 부분 추출.
  if (!closed) {
    const tail = arrStr.slice(lastEnd);
    const open = parseOpenDay(tail);
    if (open) days.push(open);
  }
  return days;
}

/** 작성 중인 날짜 조각에서 day/title/완성된 spot 들을 추출. */
function parseOpenDay(tail: string): Record<string, unknown> | null {
  const br = tail.indexOf('{');
  if (br < 0) return null;
  const s = tail.slice(br);
  const day: Record<string, unknown> = {};
  const titleM = s.match(/"title"\s*:\s*"((?:[^"\\]|\\.)*)"/);
  if (titleM) day.title = titleM[1];
  const dayM = s.match(/"day"\s*:\s*(\d+)/);
  if (dayM) day.day = parseInt(dayM[1], 10);
  const spIdx = s.search(/"spots"\s*:\s*\[/);
  if (spIdx >= 0) {
    const spStart = s.indexOf('[', spIdx);
    const spots = extractObjects(s.slice(spStart + 1));
    if (spots.length) day.spots = spots;
  }
  return (day.title || day.spots) ? day : null;
}

/** 스트리밍 중인 JSON 문자열에서 계획 일부(목적지/일수/완성된 날짜)를 best-effort 추출. */
function extractPartialPlan(raw: string): Partial<TravelPlan> | null {
  if (!raw) return null;
  let t = raw.trim();
  if (t.startsWith('```')) { const nl = t.indexOf('\n'); if (nl >= 0) t = t.slice(nl + 1); }
  const start = t.indexOf('{');
  if (start < 0) return null;
  t = t.slice(start);
  const out: Partial<TravelPlan> = {};
  const str = (k: string): string | undefined => {
    const m = t.match(new RegExp('"' + k + '"\\s*:\\s*"((?:[^"\\\\]|\\\\.)*)"'));
    return m ? m[1] : undefined;
  };
  const num = (k: string): number | undefined => {
    const m = t.match(new RegExp('"' + k + '"\\s*:\\s*(\\d+)'));
    return m ? parseInt(m[1], 10) : undefined;
  };
  out.destination = str('destination');
  out.duration = str('duration');
  out.summary = str('summary');
  out.theme = str('theme');
  out.budget = str('budget');
  out.weatherInfo = str('weatherInfo');
  out.localInfo = str('localInfo');
  const d = num('days'); if (d) out.days = d;
  const eb = num('estimatedBudgetKrw'); if (eb) out.estimatedBudgetKrw = eb;
  const itIdx = t.search(/"itinerary"\s*:\s*\[/);
  if (itIdx >= 0) {
    const arrStart = t.indexOf('[', itIdx);
    const days = extractItineraryDays(t.slice(arrStart + 1)) as TravelPlan['itinerary'];
    if (days && days.length) out.itinerary = days;
  }
  if (!out.destination && !out.itinerary) return null;
  return out;
}

export const useStreamStore = defineStore('stream', () => {
  const state = reactive<StreamState>({
    isStreaming: false,
    currentData: '',
    plan: null,
    partialPlan: null,
    flights: [],
    hotels: [],
    isComplete: false,
    error: null,
    abortController: null,
    progress: createInitialProgress(),
  });

  const resetProgress = () => {
    state.progress = createInitialProgress();
    state.currentData = '';
    state.plan = null;
    state.partialPlan = null;
    state.flights = [];
    state.hotels = [];
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
      case 'sources.flight': {
        const offers = event.payload as FlightOffer[] | undefined;
        if (Array.isArray(offers)) state.flights = offers;
        pushMessage('sources.flight', event.message, event.stage);
        break;
      }
      case 'sources.web': {
        state.progress.sources.web = (event.payload as SourceSummary) ?? null;
        pushMessage('sources.web', event.message, event.stage);
        break;
      }
      case 'sources.hotel': {
        const list = event.payload as Accommodation[] | undefined;
        if (Array.isArray(list)) state.hotels = list;
        pushMessage('sources.hotel', event.message, event.stage);
        break;
      }
      case 'model': {
        const p = event.payload as { model?: string } | string | undefined;
        const model = typeof p === 'string' ? p : p?.model ?? null;
        if (model) state.progress.model = model;
        pushMessage('model', event.message, event.stage);
        break;
      }
      case 'partial': {
        // 권역 분할 생성: 완성된 권역까지의 계획을 실시간 미리보기로.
        const p = event.payload as TravelPlan | undefined;
        if (p && typeof p === 'object') state.partialPlan = p;
        pushMessage('partial', event.message, event.stage);
        break;
      }
      case 'token': {
        const payload = event.payload;
        if (typeof payload === 'string') {
          state.currentData += payload;
          // 실시간 부분 파싱: 완성된 날짜 카드가 생기는 대로 미리보기 갱신.
          const partial = extractPartialPlan(state.currentData);
          if (partial) state.partialPlan = partial;
        }
        break;
      }
      case 'plan': {
        const payload = event.payload as TravelPlan | { fallback?: boolean; text?: string } | undefined;
        if (payload && 'fallback' in payload && payload.fallback) {
          // 구조화(전체) 파싱이 실패해도, 실시간 부분 파싱으로 완성된 날짜가 있으면 그것을 결과로 승격.
          // (생성이 중간에 잘려도 완성된 일자까지는 온전히 보여주기)
          if (state.partialPlan && state.partialPlan.itinerary && state.partialPlan.itinerary.length) {
            state.plan = state.partialPlan as TravelPlan;
          } else if (payload.text && !state.currentData) {
            state.currentData = payload.text;
          }
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

  // 저장된 계획을 결과 화면에 그대로 표시.
  const loadSaved = (plan: TravelPlan, flights: FlightOffer[]) => {
    stopStream();
    resetProgress();
    state.plan = plan;
    state.flights = flights || [];
    state.isComplete = true;
    state.isStreaming = false;
    state.progress.stage = 'completed';
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
    loadSaved,
    formatMarkdown,
    setData,
    setError,
    setStreaming,
  };
});
