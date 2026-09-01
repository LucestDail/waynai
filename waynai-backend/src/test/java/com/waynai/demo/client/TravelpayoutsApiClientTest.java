package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.FlightOfferDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TravelpayoutsApiClient} 의 네트워크 이전 가드 로직 — 토큰 게이팅, 필수 인자 검증.
 * (실제 응답 파싱은 x-access-token 필요 + 외부 API 호출이라 유닛 범위 밖)
 */
class TravelpayoutsApiClientTest {

    private final ObjectMapper om = new ObjectMapper();

    private TravelpayoutsApiClient withToken(String token) {
        TravelpayoutsApiClient c = new TravelpayoutsApiClient(om);
        ReflectionTestUtils.setField(c, "token", token);
        return c;
    }

    @Test
    void 토큰_미설정이면_비활성_그리고_빈리스트() {
        TravelpayoutsApiClient c = withToken(null);
        assertThat(c.isEnabled()).isFalse();
        List<FlightOfferDto> out = c.getCheapest("SEL", "OSA", "2026-03", null, "krw");
        assertThat(out).isEmpty(); // 비활성 → 네트워크 없이 즉시 빈 결과
    }

    @Test
    void 토큰_공백이면_비활성() {
        assertThat(withToken("   ").isEnabled()).isFalse();
    }

    @Test
    void 토큰_있으면_활성() {
        assertThat(withToken("secret-token").isEnabled()).isTrue();
    }

    @Test
    void 출발지_null이면_네트워크없이_빈리스트() {
        TravelpayoutsApiClient c = withToken("secret-token"); // 활성이지만
        assertThat(c.getCheapest(null, "OSA", "2026-03", null, "krw")).isEmpty();
    }

    @Test
    void 도착지_null이면_네트워크없이_빈리스트() {
        TravelpayoutsApiClient c = withToken("secret-token");
        assertThat(c.getCheapest("SEL", null, "2026-03", null, "krw")).isEmpty();
    }
}
