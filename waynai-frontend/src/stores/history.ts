import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { TravelPlan, FlightOffer } from '@/stores/stream';

/**
 * 저장한 여행 계획 히스토리 — **localStorage 전용**(브라우저별 저장, 인증 없음).
 * 서버 공유 저장소를 쓰지 않으므로 다른 사용자와 섞이지 않는다. (교차기기 동기화는 미지원)
 */
export interface SavedPlan {
  id: string;
  title: string;
  savedAt: number;
  plan: TravelPlan;
  flights: FlightOffer[];
}

const KEY = 'waynai.savedPlans.v1';
const MAX = 50;

function readCache(): SavedPlan[] {
  try {
    const raw = localStorage.getItem(KEY);
    const arr = raw ? JSON.parse(raw) : [];
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}

function writeCache(list: SavedPlan[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(list));
  } catch {
    /* 용량 초과 등은 무시 */
  }
}

export const useHistoryStore = defineStore('history', () => {
  const items = ref<SavedPlan[]>(readCache());

  /** localStorage 에서 최신 목록 재로딩. */
  const refresh = async () => {
    items.value = readCache();
  };

  /** 저장(localStorage). 최신순 유지, 최대 MAX 개. */
  const save = (plan: TravelPlan, flights: FlightOffer[]): SavedPlan => {
    const id = `${Date.now()}-${Math.floor(performance.now())}`;
    const title = plan.destination || plan.theme || '여행 계획';
    const entry: SavedPlan = {
      id,
      title: `${title}${plan.duration ? ' · ' + plan.duration : ''}`,
      savedAt: Date.now(),
      plan,
      flights: flights || [],
    };
    const list = [entry, ...readCache().filter((p) => p.id !== id)].slice(0, MAX);
    writeCache(list);
    items.value = list;
    return entry;
  };

  /** 상세 조회(localStorage). */
  const load = async (id: string): Promise<SavedPlan | null> => {
    return readCache().find((p) => p.id === id) || null;
  };

  const remove = (id: string) => {
    const list = readCache().filter((p) => p.id !== id);
    writeCache(list);
    items.value = list;
  };

  return { items, refresh, save, load, remove };
});
