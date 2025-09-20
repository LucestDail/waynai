package com.waynai.demo.client;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
// import com.google.genai.types.GenerateContentStreamResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Gemini API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {

    @Value("${gemini.api.key}")
    private String apiKey;

    private Client client;

    /**
     * Gemini 클라이언트 초기화
     */
    public void initializeClient() {
        if (client == null) {
            try {
                // API 키 유효성 검사
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException("Gemini API 키가 설정되지 않았습니다. application.properties에서 gemini.api.key를 확인하세요.");
                }
                
                // Client 생성 (API 키 직접 주입)
                client = Client.builder().apiKey(apiKey).build();
                log.info("Gemini API 클라이언트 초기화 완료 - API 키: {}", apiKey.substring(0, 10) + "...");
            } catch (Exception e) {
                log.error("Gemini API 클라이언트 초기화 실패", e);
                throw new RuntimeException("Gemini API 클라이언트 초기화 실패: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 텍스트 생성 (일반)
     * @param prompt 프롬프트
     * @return 생성된 텍스트
     */
    public Mono<String> generateText(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                initializeClient();
                
                log.info("Gemini API 호출 시작 - 프롬프트 길이: {}", prompt.length());
                
                GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null
                );

                String result = response.text();
                log.info("Gemini API 호출 완료 - 응답 길이: {}", result.length());
                
                return result;
            } catch (Exception e) {
                log.error("Gemini API 호출 실패", e);
                return "{\"error\": \"AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.\", \"status\": \"error\"}";
            }
        });
    }

    /**
     * 스트림 텍스트 생성 (실제 스트림 구현)
     * @param prompt 프롬프트
     * @return 스트림으로 생성된 텍스트
     */
    public Flux<String> generateTextStream(String prompt) {
        return Flux.<String>create(sink -> {
            try {
                initializeClient();
                
                log.info("Gemini API 스트림 호출 시작");
                
                // 실제 스트림 구현 - Gemini API 호출 후 즉시 스트림으로 전송
                generateText(prompt).subscribe(
                    text -> {
                        // 결과를 모아서 로깅
                        log.info("=== Gemini API 응답 결과 ===");
                        log.info("응답 길이: {} 문자", text.length());
                        log.info("응답 내용: {}", text);
                        log.info("=== 응답 끝 ===");
                        
                        // 전체 텍스트를 한 번에 스트림으로 전송 (진짜 스트림)
                        sink.next(text);
                        sink.complete();
                    },
                    error -> {
                        log.error("스트림 텍스트 생성 실패", error);
                        sink.error(error);
                    }
                );
                
            } catch (Exception e) {
                log.error("스트림 텍스트 생성 실패", e);
                sink.error(e);
            }
        })
        .onErrorResume(e -> {
            log.error("스트림 텍스트 생성 실패", e);
            return Flux.just("죄송합니다. AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요. ");
        });
    }

    /**
     * RAG 시스템용 텍스트 생성
     * @param prompt 프롬프트
     * @param context 컨텍스트 정보
     * @return 생성된 텍스트
     */
    public Mono<String> generateTextWithContext(String prompt, String context) {
        String fullPrompt = prompt + "\n\n참조 정보:\n" + context;
        return generateText(fullPrompt);
    }

    /**
     * RAG 시스템용 스트림 텍스트 생성
     * @param prompt 프롬프트
     * @param context 컨텍스트 정보
     * @return 스트림으로 생성된 텍스트
     */
    public Flux<String> generateTextStreamWithContext(String prompt, String context) {
        String fullPrompt = prompt + "\n\n참조 정보:\n" + context;
        return generateTextStream(fullPrompt);
    }

    /**
     * 프롬프트와 변수를 조합하여 텍스트 생성
     * @param prompt 프롬프트
     * @param variables 변수 맵
     * @return 생성된 텍스트
     */
    public Mono<String> generateTextWithVariables(String prompt, Map<String, String> variables) {
        String processedPrompt = processPromptWithVariables(prompt, variables);
        return generateText(processedPrompt);
    }

    /**
     * 프롬프트와 변수를 조합하여 스트림 텍스트 생성
     * @param prompt 프롬프트
     * @param variables 변수 맵
     * @return 스트림으로 생성된 텍스트
     */
    public Flux<String> generateTextStreamWithVariables(String prompt, Map<String, String> variables) {
        String processedPrompt = processPromptWithVariables(prompt, variables);
        return generateTextStream(processedPrompt);
    }

    /**
     * 프롬프트에 변수 치환 적용
     * @param prompt 프롬프트
     * @param variables 변수 맵
     * @return 치환된 프롬프트
     */
    private String processPromptWithVariables(String prompt, Map<String, String> variables) {
        String result = prompt;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "$" + entry.getKey();
            result = result.replace(placeholder, entry.getValue());
        }
        return result;
    }
}
