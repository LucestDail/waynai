package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.OpenRouterModelRouter.ResponseFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 구조화 출력(response_format) 요청 바디 구성과 강등 판정 검증.
 *
 * <p>스키마가 <b>실리지 않아도 응답은 온다</b>(json_object 로 동작). 눈으로는 차이를 알 수 없어서
 * 바디를 직접 본다. 반대로 강등 판정이 너무 넓으면 멀쩡한 구조화 출력을 영영 꺼버리므로
 * 오탐 쪽(429·5xx·타임아웃)도 함께 잠근다.
 */
class OpenRouterResponseFormatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OpenRouterModelRouter router(boolean structuredEnabled, boolean rejected) {
        OpenRouterModelRouter r = new OpenRouterModelRouter();
        ReflectionTestUtils.setField(r, "providerSort", "throughput");
        ReflectionTestUtils.setField(r, "structuredOutputsEnabled", structuredEnabled);
        ReflectionTestUtils.setField(r, "schemaRejectedByEndpoint", rejected);
        return r;
    }

    private JsonNode sampleSchema() {
        try {
            return MAPPER.readTree("{\"type\":\"object\",\"additionalProperties\":false,"
                    + "\"required\":[\"a\"],\"properties\":{\"a\":{\"type\":\"string\"}}}");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseFormat(Map<String, Object> body) {
        return (Map<String, Object>) body.get("response_format");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> provider(Map<String, Object> body) {
        return (Map<String, Object>) body.get("provider");
    }

    // ---------- 바디 구성 ----------

    @Test
    @DisplayName("스키마를 주면 response_format=json_schema(strict) 로 실린다")
    void sendsJsonSchema() {
        JsonNode schema = sampleSchema();
        Map<String, Object> body = router(true, false)
                .buildRequestBody("m", "p", ResponseFormat.jsonSchema("travel_plan", schema), false);

        Map<String, Object> rf = responseFormat(body);
        assertEquals("json_schema", rf.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> js = (Map<String, Object>) rf.get("json_schema");
        assertEquals("travel_plan", js.get("name"));
        assertEquals(Boolean.TRUE, js.get("strict"), "strict 가 아니면 스키마가 권고로만 취급된다");
        assertSame(schema, js.get("schema"), "스키마 본문이 그대로 실려야 한다");
    }

    @Test
    @DisplayName("스키마를 쓸 때는 provider.require_parameters 를 함께 보낸다")
    void requiresProviderSupport() {
        // 구조화 출력을 지원하지 않는 공급자로 라우팅되면 스키마가 조용히 무시된다.
        Map<String, Object> withSchema = router(true, false)
                .buildRequestBody("m", "p", ResponseFormat.jsonSchema("s", sampleSchema()), false);
        assertEquals(Boolean.TRUE, provider(withSchema).get("require_parameters"));
        assertEquals("throughput", provider(withSchema).get("sort"), "기존 속도 우선 라우팅은 유지한다");

        Map<String, Object> plain = router(true, false)
                .buildRequestBody("m", "p", ResponseFormat.JSON, false);
        assertNull(provider(plain).get("require_parameters"),
                "스키마가 없는데 공급자를 좁히면 폴백 폭만 줄어든다");
    }

    @Test
    @DisplayName("스키마가 없으면 기존대로 json_object, 텍스트 모드면 response_format 자체가 없다")
    void keepsLegacyModes() {
        Map<String, Object> json = router(true, false).buildRequestBody("m", "p", ResponseFormat.JSON, false);
        assertEquals(Map.of("type", "json_object"), responseFormat(json));

        Map<String, Object> text = router(true, false).buildRequestBody("m", "p", ResponseFormat.TEXT, false);
        assertNull(text.get("response_format"));
        assertNull(text.get("stream"));
    }

    @Test
    @DisplayName("스트리밍 요청에는 stream=true 가 붙고 스키마도 함께 간다")
    void streamKeepsSchema() {
        Map<String, Object> body = router(true, false)
                .buildRequestBody("m", "p", ResponseFormat.jsonSchema("s", sampleSchema()), true);
        assertEquals(Boolean.TRUE, body.get("stream"));
        assertEquals("json_schema", responseFormat(body).get("type"));
    }

    @Test
    @DisplayName("설정으로 끄면 스키마를 줘도 json_object 로 내려간다")
    void configCanDisable() {
        Map<String, Object> body = router(false, false)
                .buildRequestBody("m", "p", ResponseFormat.jsonSchema("s", sampleSchema()), false);
        assertEquals(Map.of("type", "json_object"), responseFormat(body));
        assertNull(provider(body).get("require_parameters"));
    }

    @Test
    @DisplayName("엔드포인트가 거부한 것이 실증되면 이후 요청은 스키마 없이 나간다")
    void downgradesAfterRejection() {
        Map<String, Object> body = router(true, true)
                .buildRequestBody("m", "p", ResponseFormat.jsonSchema("s", sampleSchema()), false);
        assertEquals(Map.of("type", "json_object"), responseFormat(body));
        assertFalse(router(true, true).isStructuredOutputActive());
        assertTrue(router(true, false).isStructuredOutputActive());
    }

    @Test
    @DisplayName("스키마 파일이 없으면(null) 조용히 JSON 모드로 떨어진다")
    void nullSchemaFallsBack() {
        assertSame(ResponseFormat.JSON, ResponseFormat.jsonSchema("travel_plan", null));
        assertSame(ResponseFormat.JSON, ResponseFormat.jsonSchema(null, sampleSchema()));
        assertFalse(ResponseFormat.JSON.hasSchema());
        assertSame(ResponseFormat.JSON, ResponseFormat.jsonSchema("s", sampleSchema()).withoutSchema());
    }

    // ---------- 강등 판정 ----------

    @Test
    @DisplayName("스키마 거부로 보이는 오류를 잡는다")
    void detectsSchemaRejection() {
        assertTrue(OpenRouterModelRouter.looksLikeSchemaRejection(
                "HTTP 400: {\"error\":{\"message\":\"response_format.json_schema is not supported\"}}"));
        assertTrue(OpenRouterModelRouter.looksLikeSchemaRejection(
                "HTTP 422: Unprocessable Entity - structured outputs unavailable"));
        assertTrue(OpenRouterModelRouter.looksLikeSchemaRejection(
                "HTTP 404: unknown parameter RESPONSE_FORMAT"));
    }

    @Test
    @DisplayName("스키마와 무관한 실패를 거부로 오인하지 않는다")
    void doesNotMisreadOtherFailures() {
        // 오탐하면 멀쩡한 구조화 출력을 프로세스가 죽을 때까지 꺼버린다.
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection("HTTP 429: rate limit exceeded"));
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection("HTTP 500: upstream error"));
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection("HTTP 502: bad gateway"));
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection("java.util.concurrent.TimeoutException"));
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection("HTTP 400: model is required"));
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection("빈 응답"));
        assertFalse(OpenRouterModelRouter.looksLikeSchemaRejection(null));
    }
}
