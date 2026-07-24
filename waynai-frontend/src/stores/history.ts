import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { TravelPlan, FlightOffer } from '@/stores/stream';

/**
 * 저장한 여행 계획 히스토리.
 * 서버(파일 저장소 /api/plans)를 원본으로, localStorage 는 오프라인 캐시로 사용한다.
 * 인증 없음(단일 사용자용). 서버 접근 실패 시 캐시로 폴백해 동작을 이어간다.
 */
export interface SavedPlan {
  id: string;
  title: string;
  savedAt: number;
  plan: TravelPlan;
  flights: FlightOffer[];
}

const KEY = 'waynai.savedPlans.v1';
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

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

  /** 서버 목록(요약)으로 갱신 + 캐시 병합. 서버 실패 시 캐시 유지. */
  const refresh = async () => {
    try {
      const res = await fetch(`${API_BASE}/api/plans`);
      if (!res.ok) throw new Error(String(res.status));
      const summaries: { id: string; title: string; savedAt: number }[] = await res.json();
      const cache = readCache();
      // 서버 요약을 캐시 상세와 병합(상세 plan 은 불러올 때 서버에서 채움).
      const merged: SavedPlan[] = summaries.map((s) => {
        const hit = cache.find((c) => c.id === s.id);
        return hit
          ? { ...hit, title: s.title, savedAt: s.savedAt }
          : { id: s.id, title: s.title, savedAt: s.savedAt, plan: {} as TravelPlan, flights: [] };
      });
      items.value = merged;
      writeCache(merged);
    } catch {
      items.value = readCache();
    }
  };

  /** 저장: 낙관적으로 즉시 반영 + 서버 저장(fire-and-forget). */
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
    const list = [entry, ...readCache().filter((p) => p.id !== id)].slice(0, 50);
    writeCache(list);
    items.value = list;
    fetch(`${API_BASE}/api/plans`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(entry),
    }).catch(() => {
      /* 서버 없으면 캐시에만 남는다 */
    });
    return entry;
  };

  /** 상세 조회: 캐시에 plan 이 있으면 그대로, 없으면 서버에서 로드. */
  const load = async (id: string): Promise<SavedPlan | null> => {
    const cached = readCache().find((p) => p.id === id);
    if (cached && cached.plan && Object.keys(cached.plan).length > 0) return cached;
    try {
      const res = await fetch(`${API_BASE}/api/plans/${encodeURIComponent(id)}`);
      if (!res.ok) throw new Error(String(res.status));
      const full: SavedPlan = await res.json();
      const list = [full, ...readCache().filter((p) => p.id !== id)].slice(0, 50);
      writeCache(list);
      return full;
    } catch {
      return cached || null;
    }
  };

  const remove = (id: string) => {
    const list = readCache().filter((p) => p.id !== id);
    writeCache(list);
    items.value = list;
    fetch(`${API_BASE}/api/plans/${encodeURIComponent(id)}`, { method: 'DELETE' }).catch(() => {});
  };

  return { items, refresh, save, load, remove };
});
