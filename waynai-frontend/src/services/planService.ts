import type { TravelPlan, FlightOffer } from '@/stores/stream';

/**
 * 계획 서버 보관 클라이언트.
 *
 * 소유자는 이 브라우저가 한 번 만들어 보관하는 **익명 토큰**으로만 구분한다.
 * 가입도 로그인도 없고 서버는 토큰의 해시만 갖는다(개인정보 미수집).
 *
 * ⚠️ 토큰을 잃으면 서버에 저장한 계획도 되찾을 수 없다. 다른 기기에서 같은 목록을
 * 보려면 {@link getOwnerToken} 값을 그 기기에 넣어주면 된다.
 *
 * ⚠️ 게이트웨이(nginx) Basic 인증이 Authorization 헤더를 쓰므로 토큰은 전용 헤더로 보낸다.
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const TOKEN_KEY = 'waynai.ownerToken.v1';
const OWNER_HEADER = 'X-Owner-Token';

/** 서버가 돌려주는 저장 항목(목록에서는 plan 이 비어 있다). */
export interface RemotePlan {
  id: string;
  title: string;
  savedAt: number;
  plan?: TravelPlan;
  flights?: FlightOffer[];
}

function randomToken(): string {
  // crypto.randomUUID 는 보안 컨텍스트에서만 있으므로 폴백을 둔다.
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
}

/** 이 브라우저의 소유자 토큰(없으면 만들어 보관). */
export function getOwnerToken(): string {
  try {
    let t = localStorage.getItem(TOKEN_KEY);
    if (!t) {
      t = randomToken();
      localStorage.setItem(TOKEN_KEY, t);
    }
    return t;
  } catch {
    // localStorage 를 못 쓰는 환경(프라이빗 모드 등)에서는 세션 한정 토큰으로 동작.
    return randomToken();
  }
}

/** 다른 기기의 목록을 이어받기 위해 토큰을 갈아끼운다. */
export function setOwnerToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token.trim());
}

function headers(): HeadersInit {
  return { 'Content-Type': 'application/json', [OWNER_HEADER]: getOwnerToken() };
}

/** 서버 저장. 성공하면 서버가 발급한 id 를 돌려준다. */
export async function savePlanRemote(
  plan: TravelPlan,
  flights: FlightOffer[],
): Promise<{ id: string; title: string; savedAt: number }> {
  const res = await fetch(`${API_BASE_URL}/api/plans`, {
    method: 'POST',
    headers: headers(),
    body: JSON.stringify({ plan, flights: flights || [] }),
    signal: AbortSignal.timeout(10000),
  });
  if (!res.ok) {
    throw new Error(`서버 저장 실패 (${res.status})`);
  }
  return res.json();
}

/** 내 서버 목록(최신순, 본문 없음). */
export async function listPlansRemote(): Promise<RemotePlan[]> {
  const res = await fetch(`${API_BASE_URL}/api/plans`, {
    headers: headers(),
    signal: AbortSignal.timeout(10000),
  });
  if (!res.ok) {
    throw new Error(`서버 목록 실패 (${res.status})`);
  }
  return res.json();
}

/** 내 서버 계획 1건(본문 포함). 없으면 null. */
export async function getPlanRemote(id: string): Promise<RemotePlan | null> {
  const res = await fetch(`${API_BASE_URL}/api/plans/${encodeURIComponent(id)}`, {
    headers: headers(),
    signal: AbortSignal.timeout(10000),
  });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`서버 조회 실패 (${res.status})`);
  return res.json();
}

/** 공유 링크로 열람(토큰 없이, id 를 아는 사람만). */
export async function getSharedPlan(id: string): Promise<RemotePlan | null> {
  const res = await fetch(`${API_BASE_URL}/api/plans/${encodeURIComponent(id)}/shared`, {
    signal: AbortSignal.timeout(10000),
  });
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`공유 조회 실패 (${res.status})`);
  return res.json();
}

/** 서버에서 삭제. 이미 없으면 조용히 넘어간다. */
export async function deletePlanRemote(id: string): Promise<void> {
  const res = await fetch(`${API_BASE_URL}/api/plans/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: headers(),
    signal: AbortSignal.timeout(10000),
  });
  if (!res.ok && res.status !== 404) {
    throw new Error(`서버 삭제 실패 (${res.status})`);
  }
}
