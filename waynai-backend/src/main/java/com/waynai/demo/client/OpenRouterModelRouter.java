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

    /**
     * 구조화 출력(response_format=json_schema) 사용 여부.
     * 끄면 기존 {@code json_object} 로만 동작한다(엔드포인트가 스키마를 거부할 때의 수동 탈출구).
     */
    @Value("${openrouter.structured-outputs.enabled:true}")
    private boolean structuredOutputsEnabled;

    private List<String> modelChain = Collections.emptyList();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile WebClient webClient;

    /**
     * 엔드포인트가 스키마를 거부한다는 것이 <b>실증된</b> 뒤 켜진다.
     * (스키마를 뺀 재시도가 성공 = 스키마가 원인이었다는 뜻)
     *
     * <p>여기가 켜지면 이후 요청은 스키마 없이 나간다. 매 요청마다 실패 호출을
     * 한 번씩 더 태우지 않기 위한 것이다.
     */
    private volatile boolean schemaRejectedByEndpoint = false;

    private final Map<String, Long> cooldownUntilMs = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 60_000L;

    /**
     * 응답 형식 지정.
     *
     * @param jsonMode   true 면 최소한 구문상 유효한 JSON 을 강제({@code response_format=json_object})
     * @param schemaName 구조화 출력 이름 (OpenRouter/OpenAI 규격상 필수). schema 가 있을 때만 의미.
     * @param schema     JSON Schema. null 이면 스키마 없이 jsonMode 만 적용.
     */
    public record ResponseFormat(boolean jsonMode, String schemaName, JsonNode schema) {

        public static final ResponseFormat TEXT = new ResponseFormat(false, null, null);
        public static final ResponseFormat JSON = new ResponseFormat(true, null, null);

        /** 스키마가 null 이면 조용히 JSON 모드로 떨어진다(호출부가 매번 분기하지 않도록). */
        public static ResponseFormat jsonSchema(String name, JsonNode schema) {
            if (schema == null || name == null || name.isBlank()) return JSON;
            return new ResponseFormat(true, name, schema);
        }

        public boolean hasSchema() {
            return schema != null && schemaName != null && !schemaName.isBlank();
        }

        /** 스키마만 떼어낸 형식. 엔드포인트가 스키마를 거부했을 때의 강등 대상. */
        public ResponseFormat withoutSchema() {
            return jsonMode ? JSON : TEXT;
        }
    }

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
        return generateText(prompt, onModelSelected,
                jsonMode ? ResponseFormat.JSON : ResponseFormat.TEXT);
    }

    /**
     * 응답 형식을 직접 지정하는 호출. {@link ResponseFormat#jsonSchema} 를 넘기면
     * OpenRouter 구조화 출력(response_format=json_schema, strict)을 사용한다.
     */
    public Mono<String> generateText(String prompt, Consumer<String> onModelSelected, ResponseFormat format) {
        return Mono.fromCallable(() -> invokeWithFallback(prompt, onModelSelected, format))
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
        return streamText(prompt, onModelSelected,
                jsonMode ? ResponseFormat.JSON : ResponseFormat.TEXT);
    }

    /** 응답 형식을 직접 지정하는 스트리밍 호출. */
    public Flux<String> streamText(String prompt, Consumer<String> onModelSelected, ResponseFormat format) {
        if (apiKey == null || apiKey.isBlank()) {
            return Flux.error(new IllegalStateException("OPENROUTER_API_KEY 가 설정되지 않았습니다."));
        }
        return streamWithFallback(prompt, 0, onModelSelected, format);
    }

    private Flux<String> streamWithFallback(String prompt, int idx, Consumer<String> onModelSelected,
                                            ResponseFormat format) {
        if (idx >= modelChain.size()) {
            return Flux.error(new RuntimeException("모든 OpenRouter 모델 스트리밍 실패"));
        }
        String model = modelChain.get(idx);
        ResponseFormat eff = effectiveFormat(format);
        java.util.concurrent.atomic.AtomicBoolean announced = new java.util.concurrent.atomic.AtomicBoolean(false);
        return streamOne(model, prompt, eff)
                .doOnNext(delta -> {
                    if (announced.compareAndSet(false, true)) {
                        log.info("[openrouter] 스트리밍 시작: model={}, schema={}", model,
                                eff.hasSchema() ? eff.schemaName() : "없음");
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
                    // 스키마를 거부당한 모양이면 같은 모델을 스키마 없이 한 번 더 — 모델을 건너뛰기 전에.
                    if (eff.hasSchema() && looksLikeSchemaRejection(e.getMessage())) {
                        log.warn("[openrouter] 스트리밍에서 구조화 출력이 거부됨(model={}): {} → 스키마 없이 재시도",
                                model, e.getMessage());
                        markSchemaRejected();
                        return streamWithFallback(prompt, idx, onModelSelected, eff.withoutSchema());
                    }
                    log.warn("[hot-swap:stream] {} 실패 → 다음 모델: {}", model, e.getMessage());
                    return streamWithFallback(prompt, idx + 1, onModelSelected, format);
                });
    }

    private Flux<String> streamOne(String model, String prompt, ResponseFormat format) {
        Map<String, Object> body = buildRequestBody(model, prompt, format, true);
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

    /**
     * 요청 바디 구성. 순수 함수라 테스트에서 직접 검증한다(스키마가 실제로 실리는지는
     * 눈으로 확인할 수 없고, 빠져도 json_object 로 조용히 동작하기 때문).
     */
    Map<String, Object> buildRequestBody(String model, String prompt, ResponseFormat format, boolean stream) {
        ResponseFormat eff = effectiveFormat(format);
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (stream) body.put("stream", true);
        applyProvider(body, eff.hasSchema());
        if (eff.hasSchema()) {
            // OpenRouter/OpenAI 구조화 출력. strict=true 라 스키마에 없는 키는 나오지 않는다.
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", Map.of(
                            "name", eff.schemaName(),
                            "strict", true,
                            "schema", eff.schema())));
        } else if (eff.jsonMode()) {
            // 구문상 유효한 JSON 만 강제(키·타입은 프롬프트에 의존).
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    /** 설정/실증된 거부를 반영한 실제 응답 형식. */
    private ResponseFormat effectiveFormat(ResponseFormat format) {
        ResponseFormat f = format == null ? ResponseFormat.TEXT : format;
        if (f.hasSchema() && (!structuredOutputsEnabled || schemaRejectedByEndpoint)) {
            return f.withoutSchema();
        }
        return f;
    }

    /**
     * OpenRouter 프로바이더 라우팅: 지정된 정렬 기준(기본 throughput)으로 가장 빠른 공급자 선호.
     *
     * <p>스키마를 쓸 때는 {@code require_parameters=true} 를 함께 보낸다 — 구조화 출력을
     * 지원하지 않는 공급자로 라우팅되면 스키마가 <b>조용히 무시</b>되기 때문이다.
     */
    private void applyProvider(Map<String, Object> body, boolean requireParameters) {
        boolean hasSort = providerSort != null && !providerSort.isBlank();
        if (!hasSort && !requireParameters) return;
        Map<String, Object> provider = new HashMap<>();
        if (hasSort) provider.put("sort", providerSort);
        if (requireParameters) provider.put("require_parameters", true);
        body.put("provider", provider);
    }

    /**
     * 스키마 거부로 보이는 오류인지 판정. 엔드포인트(OpenRouter 직결 / osh-ai-gateway 경유)마다
     * 문구가 다르므로 상태코드 + 키워드로 본다.
     *
     * <p>지나치게 넓게 잡으면 멀쩡한 구조화 출력을 영영 끄게 되므로 <b>4xx/501 + 응답형식 관련 낱말</b>
     * 둘 다 있어야 참으로 본다. 타임아웃·5xx·쿼터는 여기 해당하지 않는다(모델 폴백이 처리).
     */
    static boolean looksLikeSchemaRejection(String message) {
        if (message == null) return false;
        String m = message.toLowerCase();
        boolean clientError = m.contains("http 400") || m.contains("http 404") || m.contains("http 405")
                || m.contains("http 415") || m.contains("http 422") || m.contains("http 501");
        if (!clientError) return false;
        return m.contains("response_format") || m.contains("json_schema")
                || m.contains("structured output") || m.contains("structured_outputs");
    }

    private void markSchemaRejected() {
        if (!schemaRejectedByEndpoint) {
            schemaRejectedByEndpoint = true;
            log.warn("[openrouter] 이 엔드포인트({})는 구조화 출력(json_schema)을 받지 않습니다. "
                    + "이후 요청은 json_object 로 나갑니다.", baseUrl);
        }
    }

    /** 테스트·진단용: 구조화 출력이 실제로 쓰이고 있는지. */
    public boolean isStructuredOutputActive() {
        return structuredOutputsEnabled && !schemaRejectedByEndpoint;
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

    private String invokeWithFallback(String prompt, Consumer<String> onModelSelected, ResponseFormat format) {
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
                    ResponseFormat eff = effectiveFormat(format);
                    log.info("[openrouter] 호출 시도: model={}, attempt={}, jsonMode={}, schema={}, promptLen={}",
                            model, attempt, eff.jsonMode(),
                            eff.hasSchema() ? eff.schemaName() : "없음", prompt.length());
                    String text;
                    try {
                        text = callOnce(model, prompt, eff);
                    } catch (Exception callErr) {
                        // 스키마 거부로 보이면 같은 모델을 스키마 없이 한 번 더 — 성공하면 스키마가 원인이었다는
                        // 실증이므로 이후 요청은 json_object 로 보낸다. 실패하면 원 예외로 계속(모델 폴백).
                        if (!eff.hasSchema() || !looksLikeSchemaRejection(callErr.getMessage())) throw callErr;
                        log.warn("[openrouter] 구조화 출력이 거부됨(model={}): {} → 스키마 없이 재시도",
                                model, callErr.getMessage());
                        text = callOnce(model, prompt, eff.withoutSchema());
                        markSchemaRejected();
                    }
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

    private String callOnce(String model, String prompt, ResponseFormat format) throws Exception {
        Map<String, Object> body = buildRequestBody(model, prompt, format, false);

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
