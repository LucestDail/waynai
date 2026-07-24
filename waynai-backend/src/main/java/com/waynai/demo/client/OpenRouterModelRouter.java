package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * OpenRouter 핫스왑 라우터.
 *
 * <p>OpenRouter 는 OpenAI Chat Completions 호환 API 이므로
 * {@code POST https://openrouter.ai/api/v1/chat/completions} 를 WebClient 로 직접 호출한다.
 * 저가 모델부터 시도하고 실패(쿼터 초과/모델 없음/5xx 등) 시 다음 모델로 자동 전환(hot-swap)한다.
 *
 * <p>기존 GeminiModelRouter 의 핫스왑·쿨다운·재시도 구조를 그대로 계승하되, 엔드포인트/헤더/
 * 요청·응답 파싱만 OpenRouter(OpenAI) 규격으로 바꿨다.
 */
@Slf4j
@Component
public class OpenRouterModelRouter {

    @Value("${openrouter.api.key:}")
    private String apiKey;

    @Value("${openrouter.model.chain:openai/gpt-4o-mini}")
    private String modelChainRaw;

    @Value("${openrouter.retry.per-model:1}")
    private int retryPerModel;

    @Value("${openrouter.api.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${openrouter.request.timeout-seconds:60}")
    private long timeoutSeconds;

    /** OpenRouter 프로바이더 라우팅 정렬 기준: throughput(속도) | price | latency. 빈 값이면 미지정. */
    @Value("${openrouter.provider-sort:throughput}")
    private String providerSort;

    /** OpenRouter 랭킹/식별용 선택 헤더 (없어도 동작). */
    @Value("${openrouter.referer:https://waynai.app}")
    private String referer;

    @Value("${openrouter.title:WaynAI}")
    private String title;

    /** 로컬 개발 TLS 우회 (사내 MITM 프록시 대응). 운영은 false. */
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
            log.warn("OPENROUTER_API_KEY 가 비어 있습니다. AI 호출은 실패합니다. .env 를 확인하세요.");
        }
        this.modelChain = Arrays.stream(modelChainRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (modelChain.isEmpty()) {
            modelChain = List.of("openai/gpt-4o-mini");
        }
        log.info("OpenRouter 모델 체인(hot-swap 순서): {}", modelChain);
    }

    private synchronized WebClient webClient() {
        if (webClient == null) {
            WebClient.Builder builder = WebClient.builder()
                    .baseUrl(baseUrl)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .defaultHeader("HTTP-Referer", referer)
                    .defaultHeader("X-Title", title)
                    .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024));

            if (tlsInsecure) {
                log.warn("[openrouter] TLS 인증 검증을 우회합니다 (gemini.tls.insecure=true). 로컬 개발 전용.");
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
        return generateText(prompt, null);
    }

    /**
     * 핫스왑 호출 중 최종 성공한 모델명을 외부로 노출할 수 있는 변형.
     * 오케스트레이터가 SSE 로 {@code model} 이벤트를 푸시하기 위해 사용한다.
     */
    public Mono<String> generateText(String prompt, Consumer<String> onModelSelected) {
        return generateText(prompt, onModelSelected, false);
    }

    /**
     * jsonMode=true 이면 OpenRouter {@code response_format=json_object} 를 사용해
     * 구문상 유효한 JSON 만 반환하도록 강제한다. (구조화 여행 계획 전용)
     */
    public Mono<String> generateText(String prompt, Consumer<String> onModelSelected, boolean jsonMode) {
        return Mono.fromCallable(() -> invokeWithFallback(prompt, onModelSelected, jsonMode))
                .subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<String> generateTextStream(String prompt) {
        return generateText(prompt).flux();
    }

    /**
     * 진짜 토큰 스트리밍. OpenRouter {@code stream:true} 로 델타(content 조각)를 실시간 방출한다.
     * 모델 체인 순서로 시도하며, 첫 토큰이 나오기 전에 실패하면 다음 모델로 폴백한다.
     *
     * @param onModelSelected 실제로 토큰을 뱉기 시작한 모델명을 1회 통지
     * @return content 델타 문자열의 Flux (완결 시 onComplete)
     */
    public Flux<String> streamText(String prompt, Consumer<String> onModelSelected, boolean jsonMode) {
        if (apiKey == null || apiKey.isBlank()) {
            return Flux.error(new IllegalStateException("OPENROUTER_API_KEY 가 설정되지 않았습니다."));
        }
        return streamWithFallback(prompt, 0, onModelSelected, jsonMode);
    }

    private Flux<String> streamWithFallback(String prompt, int idx, Consumer<String> onModelSelected, boolean jsonMode) {
        if (idx >= modelChain.size()) {
            return Flux.error(new RuntimeException("모든 OpenRouter 모델 스트리밍 실패"));
        }
        String model = modelChain.get(idx);
        java.util.concurrent.atomic.AtomicBoolean announced = new java.util.concurrent.atomic.AtomicBoolean(false);
        return streamOne(model, prompt, jsonMode)
                .doOnNext(delta -> {
                    if (announced.compareAndSet(false, true)) {
                        log.info("[openrouter] 스트리밍 시작: model={}", model);
                        if (onModelSelected != null) {
                            try { onModelSelected.accept(model); } catch (Exception ignore) { }
                        }
                    }
                })
                .onErrorResume(e -> {
                    // 이미 토큰이 나온 뒤 끊긴 경우엔 폴백하지 않고 종료(중복 방지).
                    if (announced.get()) {
                        log.warn("[openrouter] 스트리밍 중단(부분 수신): model={}, {}", model, e.getMessage());
                        return Flux.empty();
                    }
                    log.warn("[hot-swap:stream] {} 실패 → 다음 모델: {}", model, e.getMessage());
                    return streamWithFallback(prompt, idx + 1, onModelSelected, jsonMode);
                });
    }

    private Flux<String> streamOne(String model, String prompt, boolean jsonMode) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("stream", true);
        applyProvider(body);
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return webClient()
                .post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .flatMap(b -> Mono.error(new RuntimeException(
                                "HTTP " + r.statusCode().value() + ": " + truncate(b, 300)))))
                .bodyToFlux(new org.springframework.core.ParameterizedTypeReference<org.springframework.http.codec.ServerSentEvent<String>>() {})
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .mapNotNull(sse -> extractDelta(sse.data()))
                .filter(s -> s != null && !s.isEmpty());
    }

    /** OpenRouter 프로바이더 라우팅: 지정된 정렬 기준(기본 throughput)으로 가장 빠른 공급자 선호. */
    private void applyProvider(Map<String, Object> body) {
        if (providerSort != null && !providerSort.isBlank()) {
            body.put("provider", Map.of("sort", providerSort));
        }
    }

    /** OpenRouter 스트리밍 SSE data(JSON)에서 choices[0].delta.content 추출. [DONE] 이면 null. */
    private String extractDelta(String data) {
        if (data == null || data.isBlank() || "[DONE]".equals(data.trim())) return null;
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("delta").path("content").asText("");
            }
        } catch (Exception e) {
            log.debug("[openrouter] 델타 파싱 스킵: {}", e.getMessage());
        }
        return null;
    }

    private String invokeWithFallback(String prompt, Consumer<String> onModelSelected, boolean jsonMode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY 가 설정되지 않았습니다.");
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
                    log.info("[openrouter] 호출 시도: model={}, attempt={}, jsonMode={}, promptLen={}", model, attempt, jsonMode, prompt.length());
                    String text = callOnce(model, prompt, jsonMode);
                    if (text == null || text.isBlank()) {
                        throw new RuntimeException("빈 응답");
                    }
                    log.info("[openrouter] 성공: model={}, respLen={}", model, text.length());
                    if (onModelSelected != null) {
                        try {
                            onModelSelected.accept(model);
                        } catch (Exception hookErr) {
                            log.warn("[openrouter] onModelSelected 훅 오류 (무시): {}", hookErr.getMessage());
                        }
                    }
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
        throw new RuntimeException("모든 OpenRouter 모델 호출 실패: " + (last != null ? last.getMessage() : "unknown"), last);
    }

    private String callOnce(String model, String prompt, boolean jsonMode) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        applyProvider(body);
        if (jsonMode) {
            // OpenRouter/OpenAI JSON 모드: 구문상 유효한 JSON 만 반환하도록 강제.
            body.put("response_format", Map.of("type", "json_object"));
        }

        String resp = webClient()
                .post()
                .uri("/chat/completions")
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
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText("");
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
                || msg.contains("insufficient") || msg.contains("unavailable") || msg.contains("502")
                || msg.contains("503");
    }
}
