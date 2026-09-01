package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TavilyApiClient} 의 네트워크 이전 가드 로직 — 키 게이팅, 빈/공백 질의 방어.
 */
class TavilyApiClientTest {

    private final ObjectMapper om = new ObjectMapper();

    private TavilyApiClient withKey(String key) {
        TavilyApiClient c = new TavilyApiClient(om);
        ReflectionTestUtils.setField(c, "apiKey", key);
        ReflectionTestUtils.setField(c, "apiUrl", "https://api.tavily.com/search");
        ReflectionTestUtils.setField(c, "maxResults", 5);
        return c;
    }

    @Test
    void 키_미설정이면_비활성_그리고_빈리스트() {
        TavilyApiClient c = withKey(null);
        assertThat(c.isEnabled()).isFalse();
        assertThat(c.search("파리 명소")).isEmpty(); // 비활성 → 네트워크 없이 빈 결과
    }

    @Test
    void 키_공백이면_비활성() {
        assertThat(withKey("  ").isEnabled()).isFalse();
    }

    @Test
    void 키_있으면_활성() {
        assertThat(withKey("tvly-xxx").isEnabled()).isTrue();
    }

    @Test
    void 활성이라도_질의가_null이나_blank면_네트워크없이_빈리스트() {
        TavilyApiClient c = withKey("tvly-xxx");
        assertThat(c.search(null)).isEmpty();
        assertThat(c.search("")).isEmpty();
        assertThat(c.search("   ")).isEmpty();
    }
}
