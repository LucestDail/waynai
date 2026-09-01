package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link IataResolver} 의 네트워크 비의존 경로 — 정적 오버라이드, IATA 패스스루,
 * null/blank, 캐시 히트, 질의 토큰 스캔. (autocomplete 네트워크 경로는 호출하지 않음)
 */
class IataResolverTest {

    private final IataResolver resolver = new IataResolver(new ObjectMapper());

    @Test
    void 한국어_지명_오버라이드() {
        assertThat(resolver.resolve("서울")).isEqualTo("SEL");
        assertThat(resolver.resolve("부산")).isEqualTo("PUS");
        assertThat(resolver.resolve("제주도")).isEqualTo("CJU");
        assertThat(resolver.resolve("도쿄")).isEqualTo("TYO");
    }

    @Test
    void 영문_지명_오버라이드는_대소문자_무시() {
        assertThat(resolver.resolve("Seoul")).isEqualTo("SEL");
        assertThat(resolver.resolve("BANGKOK")).isEqualTo("BKK");
        assertThat(resolver.resolve("  paris  ")).isEqualTo("PAR"); // trim
    }

    @Test
    void 이미_IATA_3대문자면_그대로_패스스루() {
        assertThat(resolver.resolve("ICN")).isEqualTo("ICN");
        assertThat(resolver.resolve("PUS")).isEqualTo("PUS");
    }

    @Test
    void 한글3음절은_IATA로_오인되지_않는다() {
        // "오사카"는 3자지만 [A-Z]{3} 아님 → 오버라이드(OSA)로 해석
        assertThat(resolver.resolve("오사카")).isEqualTo("OSA");
    }

    @Test
    void null_또는_blank는_null() {
        assertThat(resolver.resolve(null)).isNull();
        assertThat(resolver.resolve("")).isNull();
        assertThat(resolver.resolve("   ")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 캐시에_있으면_네트워크없이_반환() {
        Map<String, String> cache = (Map<String, String>) ReflectionTestUtils.getField(resolver, "cache");
        cache.put("사용자정의도시", "ZZZ");
        assertThat(resolver.resolve("사용자정의도시")).isEqualTo("ZZZ");
    }

    @Test
    void resolveFromQuery_토큰중_오버라이드_매칭() {
        assertThat(resolver.resolveFromQuery("도쿄 3박4일 커플 여행")).isEqualTo("TYO");
        assertThat(resolver.resolveFromQuery("부산 맛집 투어")).isEqualTo("PUS");
    }

    @Test
    void resolveFromQuery_null이면_null() {
        assertThat(resolver.resolveFromQuery(null)).isNull();
        assertThat(resolver.resolveFromQuery("  ")).isNull();
    }
}
