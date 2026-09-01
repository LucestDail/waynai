package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DaeroClient} 의 네트워크 비의존 로직 — 활성화 게이팅, 모드 매핑, 요약 문구 포맷.
 * (실제 HTTP 파싱은 기존 {@code DaeroClientLiveTest} 가 네트워크로 검증)
 */
class DaeroClientUnitTest {

    private final ObjectMapper om = new ObjectMapper();

    @Test
    void baseUrl_공백이면_비활성_그리고_transit는_null() {
        DaeroClient blank = new DaeroClient("", om);
        assertThat(blank.isEnabled()).isFalse();
        // 비활성 시 네트워크 접근 없이 즉시 null
        assertThat(blank.transit(37.5, 127.0, 37.4, 127.1, "09:00")).isNull();

        DaeroClient nullUrl = new DaeroClient(null, om);
        assertThat(nullUrl.isEnabled()).isFalse();
    }

    @Test
    void baseUrl_있으면_활성() {
        DaeroClient c = new DaeroClient("https://daero.example", om);
        assertThat(c.isEnabled()).isTrue();
    }

    @Test
    void modeKo_GTFS코드_한글매핑() {
        assertThat(DaeroClient.modeKo("BUS")).isEqualTo("버스");
        assertThat(DaeroClient.modeKo("SUBWAY")).isEqualTo("지하철");
        assertThat(DaeroClient.modeKo("RAIL")).isEqualTo("기차");
        assertThat(DaeroClient.modeKo("AIR")).isEqualTo("항공");
        assertThat(DaeroClient.modeKo("FERRY")).isEqualTo("여객선");
        assertThat(DaeroClient.modeKo("UNKNOWN_MODE")).isEqualTo("대중교통"); // default
    }

    @Test
    void transit_note_요약문구_포맷() {
        DaeroClient.Transit t = new DaeroClient.Transit(53, 1, 1450, "SUBWAY", "2호선", "지하철·버스");
        String note = t.note();
        assertThat(note)
                .contains("대중교통")
                .contains("53분")
                .contains("환승 1회")
                .contains("1,450원")   // 천단위 콤마
                .contains("지하철·버스")
                .contains("2호선");
    }

    @Test
    void transit_note_modeSummary_비면_firstMode로_대체_그리고_라우트없음() {
        DaeroClient.Transit t = new DaeroClient.Transit(30, 0, 1250, "BUS", "", "");
        String note = t.note();
        assertThat(note).contains("30분").contains("환승 0회").contains("1,250원").contains("버스");
        // firstRoute 공백 → 괄호 안에 라우트 문자열 없이 수단만
        assertThat(note).endsWith("(버스)");
    }
}
