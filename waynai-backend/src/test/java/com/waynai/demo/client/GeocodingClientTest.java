package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link GeocodingClient} 의 네트워크 비의존 경로 — 입력 방어, 캐시 히트, 음성결과 sentinel.
 */
class GeocodingClientTest {

    private final GeocodingClient client = new GeocodingClient(new ObjectMapper());

    @Test
    void 이름이_null이나_blank면_null() {
        assertThat(client.geocode(null, "대한민국")).isNull();
        assertThat(client.geocode("", "대한민국")).isNull();
        assertThat(client.geocode("   ", "대한민국")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 캐시히트_좌표면_네트워크없이_반환() {
        Map<String, double[]> cache = (Map<String, double[]>) ReflectionTestUtils.getField(client, "cache");
        // key = (name + ", " + regionHint).toLowerCase()
        cache.put("경복궁, 서울", new double[]{37.5796, 126.9770});
        double[] p = client.geocode("경복궁", "서울");
        assertThat(p).isNotNull();
        assertThat(p[0]).isEqualTo(37.5796);
        assertThat(p[1]).isEqualTo(126.9770);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 이전실패_sentinel이_캐시되어있으면_null() {
        Map<String, double[]> cache = (Map<String, double[]>) ReflectionTestUtils.getField(client, "cache");
        cache.put("없는장소, 서울", new double[0]); // 길이0 sentinel = 이전 실패
        assertThat(client.geocode("없는장소", "서울")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 지역힌트_없이도_키가_구성된다() {
        Map<String, double[]> cache = (Map<String, double[]>) ReflectionTestUtils.getField(client, "cache");
        cache.put("남산타워", new double[]{37.5512, 126.9882});
        double[] p = client.geocode("남산타워", null);
        assertThat(p).isNotNull();
        assertThat(p[0]).isEqualTo(37.5512);
    }
}
