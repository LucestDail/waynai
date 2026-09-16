package com.waynai.demo.client;

import com.waynai.demo.client.OpenRouterModelRouter.ResponseFormat;
import com.waynai.demo.service.TravelOrchestratorService;
import com.waynai.demo.util.JsonSchemaLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 스키마 이름 → 실제 스키마 전달 배선 검증.
 *
 * <p>여기가 조용히 끊기기 쉬운 자리다. 스키마 파일 이름을 바꾸거나 경로를 옮기면
 * 로더가 null 을 주고 호출은 그냥 {@code json_object} 로 나간다 — <b>에러도, 화면 차이도 없다.</b>
 * 그래서 "이름을 주면 스키마가 실제로 실린다" 와 "계획 생성이 쓰는 이름이 실재한다" 를 잠근다.
 */
class GeminiApiClientSchemaTest {

    private final OpenRouterModelRouter router = mock(OpenRouterModelRouter.class);
    private final JsonSchemaLoader loader = new JsonSchemaLoader();   // 실물 로더
    private final GeminiApiClient client = new GeminiApiClient(router, loader);

    private ResponseFormat captureNonStreaming(String schemaName) {
        when(router.generateText(anyString(), any(), any(ResponseFormat.class))).thenReturn(Mono.just("{}"));
        client.generateJson("prompt", null, schemaName).block();
        ArgumentCaptor<ResponseFormat> captor = ArgumentCaptor.forClass(ResponseFormat.class);
        verify(router).generateText(anyString(), any(), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("계획 생성이 쓰는 스키마 이름이 실재한다 — 이름이 어긋나면 구조화 출력이 조용히 꺼진다")
    void planSchemaNameResolves() {
        assertNotNull(loader.load(TravelOrchestratorService.PLAN_SCHEMA),
                TravelOrchestratorService.PLAN_SCHEMA + ".schema.json 을 찾지 못했다");
    }

    @Test
    @DisplayName("스키마 이름을 주면 그 스키마가 요청에 실린다")
    void passesSchemaWhenNamed() {
        ResponseFormat f = captureNonStreaming(TravelOrchestratorService.PLAN_SCHEMA);

        assertTrue(f.hasSchema(), "이름을 줬는데 스키마가 안 실렸다");
        assertEquals(TravelOrchestratorService.PLAN_SCHEMA, f.schemaName());
        assertTrue(f.schema().path("properties").has("itinerary"), "다른 스키마가 실렸다");
    }

    @Test
    @DisplayName("스트리밍 경로도 같은 스키마를 싣는다 — 계획 생성은 스트리밍으로 돈다")
    void streamPassesSchemaToo() {
        when(router.streamText(anyString(), any(), any(ResponseFormat.class))).thenReturn(Flux.just("{}"));

        client.generateJsonStream("prompt", (Consumer<String>) null,
                TravelOrchestratorService.PLAN_SCHEMA).blockLast();

        ArgumentCaptor<ResponseFormat> captor = ArgumentCaptor.forClass(ResponseFormat.class);
        verify(router).streamText(anyString(), any(), captor.capture());
        assertTrue(captor.getValue().hasSchema());
        assertEquals(TravelOrchestratorService.PLAN_SCHEMA, captor.getValue().schemaName());
    }

    @Test
    @DisplayName("이름이 없거나 파일이 없으면 기존 JSON 모드로 떨어진다(터지지 않는다)")
    void fallsBackWhenSchemaMissing() {
        assertFalse(captureNonStreaming(null).hasSchema());

        reset(router);
        assertFalse(captureNonStreaming("없는_스키마_이름").hasSchema(),
                "없는 스키마를 그대로 실으면 매 요청이 400 으로 튕긴다");
    }
}
