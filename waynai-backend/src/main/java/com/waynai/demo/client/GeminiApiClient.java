package com.waynai.demo.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Gemini API 클라이언트.
 *
 * <p>실제 호출은 {@link OpenRouterModelRouter} 가 담당하며, 이 클라이언트는 기존 서비스들이
 * 사용하던 인터페이스(프롬프트 변수 치환, RAG 컨텍스트 주입 등)를 유지하기 위한
 * 얇은 어댑터 역할만 수행합니다.
 *
 * <p>모델 체인·fallback·쿨다운 로직은 전부 {@link OpenRouterModelRouter} 쪽에서 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    // LLM 백엔드: OpenRouter (OpenAI 호환). 과거 GeminiModelRouter 대체.
    private final OpenRouterModelRouter router;

    public Mono<String> generateText(String prompt) {
        return router.generateText(prompt)
                .onErrorResume(e -> {
                    log.error("Gemini 호출 전체 실패: {}", e.getMessage());
                    return Mono.just("{\"error\":\"AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.\",\"status\":\"error\"}");
                });
    }

    /**
     * 핫스왑 중 최종 선택된 모델명을 콜백으로 노출하는 호출 변형.
     * SSE 오케스트레이터가 {@code model} 이벤트 푸시용으로 사용합니다.
     */
    public Mono<String> generateText(String prompt, Consumer<String> onModelSelected) {
        return router.generateText(prompt, onModelSelected)
                .onErrorResume(e -> {
                    log.error("LLM 호출 전체 실패: {}", e.getMessage());
                    return Mono.just("{\"error\":\"AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.\",\"status\":\"error\"}");
                });
    }

    /**
     * JSON 모드 강제 호출. 저가 모델이 자리표시자/불완전 JSON 을 뱉는 것을 방지하기 위해
     * 구조화 여행 계획 생성에만 사용한다. (채팅 등 자유 텍스트 호출에는 쓰지 말 것)
     */
    public Mono<String> generateJson(String prompt) {
        return generateJson(prompt, null);
    }

    /**
     * JSON 모드 + 진짜 토큰 스트리밍. content 델타를 실시간 Flux 로 방출한다.
     * 구조화 여행 계획 생성 시 체감 지연을 줄이기 위해 사용.
     */
    public reactor.core.publisher.Flux<String> generateJsonStream(String prompt, Consumer<String> onModelSelected) {
        return router.streamText(prompt, onModelSelected, true)
                .onErrorResume(e -> {
                    log.error("LLM(JSON stream) 호출 전체 실패: {}", e.getMessage());
                    return reactor.core.publisher.Flux.empty();
                });
    }

    public Mono<String> generateJson(String prompt, Consumer<String> onModelSelected) {
        return router.generateText(prompt, onModelSelected, true)
                .onErrorResume(e -> {
                    log.error("LLM(JSON) 호출 전체 실패: {}", e.getMessage());
                    return Mono.just("{\"error\":\"AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.\",\"status\":\"error\"}");
                });
    }

    public Flux<String> generateTextStream(String prompt) {
        return router.generateTextStream(prompt)
                .onErrorResume(e -> {
                    log.error("Gemini 스트림 호출 전체 실패: {}", e.getMessage());
                    return Flux.just("죄송합니다. AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
                });
    }

    public Mono<String> generateTextWithContext(String prompt, String context) {
        return generateText(prompt + "\n\n참조 정보:\n" + context);
    }

    public Flux<String> generateTextStreamWithContext(String prompt, String context) {
        return generateTextStream(prompt + "\n\n참조 정보:\n" + context);
    }

    public Mono<String> generateTextWithVariables(String prompt, Map<String, String> variables) {
        return generateText(processPromptWithVariables(prompt, variables));
    }

    public Flux<String> generateTextStreamWithVariables(String prompt, Map<String, String> variables) {
        return generateTextStream(processPromptWithVariables(prompt, variables));
    }

    private String processPromptWithVariables(String prompt, Map<String, String> variables) {
        String result = prompt;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("$" + entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
}
