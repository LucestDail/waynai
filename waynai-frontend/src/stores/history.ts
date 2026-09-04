import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { TravelPlan, FlightOffer } from '@/stores/stream';
import {
  savePlanRemote,
  listPlansRemote,
  getPlanRemote,
  deletePlanRemote,
} from '@/services/planService';

/**
 * 저장한 여행 계획 히스토리 — **localStorage + 서버 하이브리드**.
 *
 * 원래 localStorage 전용이라 기기를 바꾸면 계획이 사라졌다. 서버 보관이 붙은 뒤로도
 * 로컬을 계속 쓴다:
 * - **로컬을 먼저 쓴다** → 저장이 즉시 반영되고, 서버가 죽거나 오프라인이어도 동작한다
 * - **서버는 뒤따라 올린다** → 성공하면 `remoteId` 가 붙고 다른 기기에서도 보인다
 * - 업로드에 실패한 항목은 다음 `refresh()` 에서 다시 시도한다
 *
 * 소유자 구분은 익명 토큰(가입·로그인 없음)이며 상세는 `services/planService.ts` 참고.
 */
export interface SavedPlan {
  id: string;
  title: string;
  savedAt: number;
  plan: TravelPlan;
  flights: FlightOffer[];
  /** 서버에 올라간 경우의 서버측 id. 없으면 아직 이 기기에만 있다. */
  remoteId?: string;
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

  /** 서버 동기화 상태 — UI 에서 안내용으로 쓸 수 있다. */
  const syncing = ref(false);
  const syncError = ref<string | null>(null);

  const commit = (list: SavedPlan[]) => {
    writeCache(list);
    items.value = list;
  };

  /** 아직 서버에 없는 항목을 올린다(실패는 조용히 넘긴다 — 다음 refresh 에서 재시도). */
  const uploadPending = async () => {
    const list = readCache();
    let changed = false;
    for (const entry of list) {
      if (entry.remoteId) continue;
      try {
        const res = await savePlanRemote(entry.plan, entry.flights);
        entry.remoteId = res.id;
        changed = true;
      } catch {
        break; // 서버가 안 되는 상황이면 나머지도 실패한다 — 빨리 포기
      }
    }
    if (changed) commit(list);
  };

  /**
   * 목록 재로딩 — 로컬을 기준으로 두고, 서버에만 있는 것(다른 기기 저장분)을 합친다.
   * 서버가 안 되면 로컬만으로 조용히 동작한다.
   */
  const refresh = async () => {
    const local = readCache();
    items.value = local; // 서버를 기다리지 않고 먼저 보여준다

    syncing.value = true;
    syncError.value = null;
    try {
      await uploadPending();

      const remote = await listPlansRemote();
      const known = new Set(readCache().map((p) => p.remoteId).filter(Boolean));
      // 서버에만 있는 항목 = 다른 기기에서 저장한 것. 본문은 열 때 받아온다.
      const fromOthers: SavedPlan[] = remote
        .filter((r) => !known.has(r.id))
        .map((r) => ({
          id: r.id,
          title: r.title,
          savedAt: r.savedAt,
          plan: {} as TravelPlan, // 본문은 load(id) 에서 지연 로딩
          flights: [],
          remoteId: r.id,
        }));

      if (fromOthers.length) {
        const merged = [...readCache(), ...fromOthers]
          .sort((a, b) => (b.savedAt || 0) - (a.savedAt || 0))
          .slice(0, MAX);
        commit(merged);
      } else {
        items.value = readCache();
      }
    } catch (e) {
      // 서버 동기화 실패는 기능 실패가 아니다 — 로컬 목록은 그대로 쓴다.
      syncError.value = e instanceof Error ? e.message : '서버 동기화 실패';
    } finally {
      syncing.value = false;
    }
  };

  /**
   * 저장 — 로컬에 즉시 쓰고 서버 업로드는 뒤따라 보낸다.
   * 반환은 로컬 항목이며, 업로드 성공 시 `remoteId` 가 나중에 채워진다.
   */
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
    commit([entry, ...readCache().filter((p) => p.id !== id)].slice(0, MAX));

    // 업로드는 기다리지 않는다(저장 버튼이 서버 응답에 묶이지 않게).
    savePlanRemote(plan, flights || [])
      .then((res) => {
        const list = readCache();
        const found = list.find((p) => p.id === id);
        if (found) {
          found.remoteId = res.id;
          commit(list);
        }
      })
      .catch(() => {
        /* 다음 refresh 에서 재시도 */
      });

    return entry;
  };

  /** 상세 조회 — 로컬에 본문이 있으면 그것, 없으면(다른 기기 저장분) 서버에서 받아온다. */
  const load = async (id: string): Promise<SavedPlan | null> => {
    const local = readCache().find((p) => p.id === id) || null;
    const hasBody = local?.plan && Object.keys(local.plan).length > 0;
    if (local && hasBody) {
      return local;
    }

    const remoteId = local?.remoteId || id;
    try {
      const remote = await getPlanRemote(remoteId);
      if (!remote?.plan) return local;

      // 받아온 본문을 로컬에 채워 넣어 다음부터는 오프라인에서도 열린다.
      const list = readCache();
      const target = list.find((p) => p.id === id);
      if (target) {
        target.plan = remote.plan;
        target.flights = remote.flights || [];
        commit(list);
        return target;
      }
      return {
        id: remote.id,
        title: remote.title,
        savedAt: remote.savedAt,
        plan: remote.plan,
        flights: remote.flights || [],
        remoteId: remote.id,
      };
    } catch {
      return local;
    }
  };

  /** 삭제 — 로컬에서 지우고 서버에도 반영한다. */
  const remove = (id: string) => {
    const target = readCache().find((p) => p.id === id);
    commit(readCache().filter((p) => p.id !== id));
    if (target?.remoteId) {
      deletePlanRemote(target.remoteId).catch(() => {
        /* 서버 삭제 실패는 무시 — 로컬에서는 이미 사라졌다 */
      });
    }
  };

  return { items, syncing, syncError, refresh, save, load, remove };
});
