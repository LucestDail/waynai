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
 * <p>실제 호출은 {@link GeminiModelRouter} 가 담당하며, 이 클라이언트는 기존 서비스들이
 * 사용하던 인터페이스(프롬프트 변수 치환, RAG 컨텍스트 주입 등)를 유지하기 위한
 * 얇은 어댑터 역할만 수행합니다.
 *
 * <p>모델 체인·fallback·쿨다운 로직은 전부 {@link GeminiModelRouter} 쪽에서 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    private final GeminiModelRouter router;

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
                    log.error("Gemini 호출 전체 실패: {}", e.getMessage());
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
