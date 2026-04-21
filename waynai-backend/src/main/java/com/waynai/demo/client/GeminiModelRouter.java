package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gemini 핫스왑 라우터.
 *
 * <p>저가 모델부터 차례로 호출을 시도하고, 실패(쿼터 초과, 모델 없음, 5xx 등)가
 * 발생하면 다음 모델로 자동 전환(hot-swap)합니다. Google genai SDK 대신 공식
 * REST 엔드포인트({@code generativelanguage.googleapis.com})를 WebClient 로 직접
 * 호출하여, 일부 환경에서 발생하는 SDK 내부 HTTP 계층 실패를 회피합니다.
 */
@Slf4j
@Component
public class GeminiModelRouter {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model.chain:gemini-2.5-flash-lite,gemini-2.0-flash-lite,gemini-2.0-flash,gemini-2.5-flash,gemini-2.5-pro}")
    private String modelChainRaw;

    @Value("${gemini.retry.per-model:1}")
    private int retryPerModel;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${gemini.request.timeout-seconds:60}")
    private long timeoutSeconds;

    /**
     * 로컬 개발 환경에서 JVM cacerts 에 Google 루트 CA 가 누락되어 TLS 핸드셰이크가
     * 실패하는 경우 임시 우회 옵션. 기본값은 false. 운영에서는 반드시 false 로 유지해야 함.
     */
    @Value("${gemini.tls.insecure:false}")
    private boolean tlsInsecure;

    private List<String> modelChain = Collections.emptyList();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile WebClient webClient;

    private final Map<String, Long> cooldownUntilMs = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 60_000L;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY 가 비어 있습니다. AI 호출은 실패합니다. .env 를 확인하세요.");
        }
        this.modelChain = Arrays.stream(modelChainRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (modelChain.isEmpty()) {
            modelChain = List.of("gemini-2.0-flash");
        }
        log.info("Gemini 모델 체인(hot-swap 순서): {}", modelChain);
    }

    private synchronized WebClient webClient() {
        if (webClient == null) {
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(baseUrl)
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));

            if (tlsInsecure) {
                log.warn("[gemini] TLS 인증 검증을 우회합니다 (gemini.tls.insecure=true). 로컬 개발 전용 - 운영에서 사용하지 마세요.");
                try {
                    SslContext sslContext = SslContextBuilder.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build();
                    HttpClient httpClient = HttpClient.create().secure(spec -> spec.sslContext(sslContext));
                    builder.clientConnector(new ReactorClientHttpConnector(httpClient));
                } catch (Exception e) {
                    log.error("InsecureTrustManager 설정 실패. 기본 TLS 로 진행합니다.", e);
                }
            }

            webClient = builder.build();
        }
        return webClient;
    }

    public List<String> getModelChain() {
        return Collections.unmodifiableList(modelChain);
    }

    public Mono<String> generateText(String prompt) {
        return Mono.fromCallable(() -> invokeWithFallback(prompt))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<String> generateTextStream(String prompt) {
        return generateText(prompt).flux();
    }

    private String invokeWithFallback(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY 가 설정되지 않았습니다.");
        }
        Exception last = null;
        long now = System.currentTimeMillis();
        for (String model : modelChain) {
            Long coolUntil = cooldownUntilMs.get(model);
            if (coolUntil != null && coolUntil > now) {
                log.info("[hot-swap] {} 은 쿨다운 중이라 건너뜁니다 (남은 {}ms)", model, coolUntil - now);
                continue;
            }
            for (int attempt = 0; attempt <= retryPerModel; attempt++) {
                try {
                    log.info("[gemini] 호출 시도: model={}, attempt={}, promptLen={}", model, attempt, prompt.length());
                    String text = callOnce(model, prompt);
                    if (text == null || text.isBlank()) {
                        throw new RuntimeException("빈 응답");
                    }
                    log.info("[gemini] 성공: model={}, respLen={}", model, text.length());
                    return text;
                } catch (Exception e) {
                    last = e;
                    boolean isQuota = isQuotaOrRateLimit(e);
                    log.warn("[hot-swap] {} 실패 (attempt={}): {} -> {}", model, attempt, e.getClass().getSimpleName(), e.getMessage());
                    if (isQuota) {
                        cooldownUntilMs.put(model, System.currentTimeMillis() + COOLDOWN_MS);
                        break;
                    }
                }
            }
        }
        throw new RuntimeException("모든 Gemini 모델 호출 실패: " + (last != null ? last.getMessage() : "unknown"), last);
    }

    private String callOnce(String model, String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        String resp = webClient()
                .post()
                .uri(uri -> uri.path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(b -> Mono.error(new RuntimeException(
                                "HTTP " + r.statusCode().value() + ": " + truncate(b, 500)))))
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

        return extractText(resp);
    }

    private String extractText(String json) throws Exception {
        if (json == null || json.isBlank()) return "";
        JsonNode root = objectMapper.readTree(json);
        JsonNode cands = root.path("candidates");
        if (cands.isArray() && cands.size() > 0) {
            JsonNode parts = cands.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText("");
            }
        }
        return "";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private boolean isQuotaOrRateLimit(Throwable e) {
        String msg = (e.getMessage() == null ? "" : e.getMessage()).toLowerCase();
        return msg.contains("quota") || msg.contains("rate") || msg.contains("429")
                || msg.contains("resource_exhausted") || msg.contains("unavailable");
    }
}
