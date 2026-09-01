package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.util.AreaCodeUtil;
import com.waynai.demo.util.PromptLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link IntentAnalysisService#analyzeIntent} 의 LLM 응답 처리 로직 —
 * 정상 파싱, 코드펜스 제거, API 오류/파싱실패 폴백, "null" 문자열 정화, 프롬프트 부재.
 * (LLM/네트워크는 GeminiApiClient 모킹으로 대체)
 */
class IntentAnalysisServiceTest {

    private GeminiApiClient gemini;
    private PromptLoader promptLoader;
    private AreaCodeUtil areaCodeUtil;
    private NaverSearchService naverSearchService;
    private IntentAnalysisService service;

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiApiClient.class);
        promptLoader = mock(PromptLoader.class);
        areaCodeUtil = mock(AreaCodeUtil.class);
        naverSearchService = mock(NaverSearchService.class);
        when(promptLoader.getPrompt("intent_analysis")).thenReturn("분석: $query\n지역:$areaData");
        when(areaCodeUtil.getAllAreaCodes()).thenReturn(List.of());
        service = new IntentAnalysisService(gemini, promptLoader,
                new ObjectMapper(), areaCodeUtil, naverSearchService);
    }

    @Test
    void 정상_JSON이면_그대로_파싱() {
        when(gemini.generateJson(anyString())).thenReturn(Mono.just(
                "{\"intent\":\"area_keyword\",\"keyword\":\"맛집\",\"destination\":\"부산\",\"days\":3}"));
        StepVerifier.create(service.analyzeIntent("부산 맛집 여행"))
                .assertNext(dto -> {
                    assertThat(dto.getIntent()).isEqualTo("area_keyword");
                    assertThat(dto.getKeyword()).isEqualTo("맛집");
                    assertThat(dto.getDestination()).isEqualTo("부산");
                    assertThat(dto.getDays()).isEqualTo(3);
                })
                .verifyComplete();
    }

    @Test
    void 코드펜스로_감싼_JSON도_정리해_파싱() {
        String fenced = "```json\n{\"intent\":\"area\",\"destination\":\"제주\"}\n```";
        when(gemini.generateJson(anyString())).thenReturn(Mono.just(fenced));
        StepVerifier.create(service.analyzeIntent("제주 여행"))
                .assertNext(dto -> {
                    assertThat(dto.getIntent()).isEqualTo("area");
                    assertThat(dto.getDestination()).isEqualTo("제주");
                })
                .verifyComplete();
    }

    @Test
    void API_오류응답이면_기본_general로_폴백() {
        // GeminiApiClient 가 실패 시 반환하는 형태: error+status 포함
        when(gemini.generateJson(anyString())).thenReturn(Mono.just(
                "{\"error\":\"일시적 문제\",\"status\":\"error\"}"));
        StepVerifier.create(service.analyzeIntent("아무 질의"))
                .assertNext(dto -> {
                    assertThat(dto.getIntent()).isEqualTo("general");
                    assertThat(dto.getConfidence()).isEqualTo(0.5);
                    assertThat(dto.getReason()).contains("일시적 오류");
                })
                .verifyComplete();
    }

    @Test
    void 깨진_JSON이면_파싱실패_폴백() {
        when(gemini.generateJson(anyString())).thenReturn(Mono.just("{intent: 부산 이런건 JSON 아님"));
        StepVerifier.create(service.analyzeIntent("부산"))
                .assertNext(dto -> {
                    assertThat(dto.getIntent()).isEqualTo("general");
                    assertThat(dto.getReason()).contains("파싱 오류");
                })
                .verifyComplete();
    }

    @Test
    void 문자열_null을_JSON_null로_정화해_파싱실패_방지() {
        // LLM 이 area 필드에 문자열 "null" 을 넣어도 전체 intent 가 날아가지 않아야 한다
        when(gemini.generateJson(anyString())).thenReturn(Mono.just(
                "{\"intent\":\"area\",\"area\":\"null\",\"days\":2}"));
        StepVerifier.create(service.analyzeIntent("여행"))
                .assertNext(dto -> {
                    assertThat(dto.getIntent()).isEqualTo("area");
                    assertThat(dto.getArea()).isNull();
                    assertThat(dto.getDays()).isEqualTo(2); // 다른 필드는 살아있음
                })
                .verifyComplete();
    }

    @Test
    void 프롬프트가_없으면_에러_Mono() {
        when(promptLoader.getPrompt("intent_analysis")).thenReturn(null);
        StepVerifier.create(service.analyzeIntent("부산"))
                .expectError(RuntimeException.class)
                .verify();
    }
}
