package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * daero 연계의 실제 경로(네트워크 + JSON 파싱)를 라이브 엔진으로 검증한다.
 * waynai 오케스트레이터의 국내 대중교통 보강(enrichTransit)이 의존하는 유일한 외부 코드가 DaeroClient 이므로,
 * 이 클라이언트가 실 응답을 올바르게 파싱하면 연계가 동작함을 보장한다.
 *
 * <p>네트워크 의존 테스트 — daero 미기동/불가 시 assumeTrue 로 건너뛴다(오프라인 빌드 안전).
 * 로컬 daero 가 있으면 DAERO_TEST_BASE_URL 로 오버라이드 가능.
 */
class DaeroClientLiveTest {

    private static final String BASE = System.getenv().getOrDefault("DAERO_TEST_BASE_URL", "https://daero.duckdns.org");

    @Test
    void 서울_두지점_대중교통_요약을_파싱한다() {
        DaeroClient client = new DaeroClient(BASE, new ObjectMapper());
        assertThat(client.isEnabled()).isTrue();

        // 서울역(37.5665,126.9780) → 강남역(37.4979,127.0276)
        DaeroClient.Transit t = client.transit(37.5665, 126.9780, 37.4979, 127.0276, "09:00");
        assumeTrue(t != null, "daero 미응답/무경로 → 테스트 skip");

        assertThat(t.durationMin()).isGreaterThan(0);
        assertThat(t.transfers()).isGreaterThanOrEqualTo(0);
        assertThat(t.fareKrw()).isGreaterThanOrEqualTo(0);
        // enrichTransit 이 각 날짜 교통 문구에 붙이는 요약 문자열
        assertThat(t.note()).contains("대중교통").contains("분").contains("원");
        System.out.println("[daero-live] " + t.note());
    }

    @Test
    void 좌표에_경로가_없으면_null() {
        DaeroClient client = new DaeroClient(BASE, new ObjectMapper());
        // 바다 한가운데(정류장 없음) → found=false → null
        DaeroClient.Transit t = client.transit(34.0000, 125.0000, 34.0100, 125.0100, "09:00");
        assertThat(t).isNull();
    }
}
