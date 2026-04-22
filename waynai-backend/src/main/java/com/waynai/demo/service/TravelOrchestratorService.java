package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.NaverBlogSearchDto;
import com.waynai.demo.dto.SourceSummaryDto;
import com.waynai.demo.dto.TouristSpotDto;
import com.waynai.demo.dto.TouristSpotResponseDto;
import com.waynai.demo.dto.TravelEvent;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 여행 계획 생성 오케스트레이터.
 *
 * <p>프론트의 진행 상태 UI 가 실시간으로 "의도 분석 → 컨텍스트 수집 → 모델 선택 →
 * 토큰 스트림 → 구조화 계획" 을 볼 수 있도록, 각 단계를 {@link TravelEvent} 로 푸시합니다.
 *
 * <p>기존 {@link TravelPlanService} 가 제공하던 구조화 JSON 프롬프트를 재사용하되,
 * Gemini 호출이 완료된 시점에 구조화 파싱을 시도하고 성공하면 {@code plan} 이벤트,
 * 실패하면 누적된 {@code token} 텍스트가 그대로 결과로 남도록 설계했습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelOrchestratorService {

    private final IntentAnalysisService intentAnalysisService;
    private final NaverSearchService naverSearchService;
    private final TouristInfoService touristInfoService;
    private final GeminiApiClient geminiApiClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    /**
     * 사용자 질의로부터 Flux<TravelEvent> 파이프라인을 생성한다.
     * 호출자(Controller)는 이 Flux 를 그대로 SSE 로 흘려보내면 된다.
     */
    public Flux<TravelEvent> generatePlanStream(String query) {
        return Flux.defer(() -> {
            Sinks.Many<TravelEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
            // 별도 스레드에서 파이프라인 실행. 각 단계에서 sink 로 이벤트 push.
            CompletableFuture.runAsync(() -> runPipeline(query, sink),
                    Schedulers.boundedElastic()::schedule);
            return sink.asFlux();
        });
    }

    private void runPipeline(String query, Sinks.Many<TravelEvent> sink) {
        try {
            emit(sink, TravelEvent.builder()
                    .type("stage").stage("analyzing")
                    .message("여행 의도를 분석하는 중입니다.")
                    .build());

            IntentAnalysisDto intent = intentAnalysisService.analyzeIntent(query)
                    .onErrorResume(e -> {
                        log.warn("[orchestrator] intent 분석 실패, 기본값 사용: {}", e.getMessage());
                        return reactor.core.publisher.Mono.just(IntentAnalysisDto.builder()
                                .intent("general")
                                .confidence(0.3)
                                .reason("intent 분석 실패: " + e.getMessage())
                                .build());
                    })
                    .block();

            emit(sink, TravelEvent.builder()
                    .type("intent").stage("analyzing")
                    .message(describeIntent(intent))
                    .payload(intent)
                    .build());

            emit(sink, TravelEvent.builder()
                    .type("stage").stage("searching")
                    .message("RAG 컨텍스트(관광공사 · 네이버 블로그)를 병렬로 수집하는 중입니다.")
                    .build());

            // 병렬 수집 (실패해도 파이프라인은 계속 진행).
            CompletableFuture<TouristSpotResponseDto> tourFut = CompletableFuture.supplyAsync(
                    () -> safeCollectTour(intent));
            CompletableFuture<NaverBlogSearchDto> naverFut = CompletableFuture.supplyAsync(
                    () -> safeCollectNaver(query));
            CompletableFuture.allOf(tourFut, naverFut).join();

            TouristSpotResponseDto tour = tourFut.get();
            NaverBlogSearchDto naver = naverFut.get();

            emit(sink, TravelEvent.builder()
                    .type("sources.tour").stage("searching")
                    .message(tour != null && tour.isSuccess()
                            ? String.format("관광공사 API 에서 관광지 %d개를 찾았습니다.",
                                    nullSafe(tour.getItemCount()))
                            : "관광공사 API 호출 결과가 없습니다.")
                    .payload(toTourSummary(intent, tour))
                    .build());

            emit(sink, TravelEvent.builder()
                    .type("sources.naver").stage("searching")
                    .message(naver != null && naver.getItems() != null
                            ? String.format("네이버 블로그에서 참고 포스트 %d개를 찾았습니다.",
                                    naver.getItems().size())
                            : "네이버 블로그 검색 결과가 없습니다.")
                    .payload(toNaverSummary(query, naver))
                    .build());

            // === Gemini 호출 ===
            emit(sink, TravelEvent.builder()
                    .type("stage").stage("generating")
                    .message("Gemini 모델을 통해 여행 계획을 생성하는 중입니다.")
                    .build());

            String prompt = buildStructuredPrompt(intent, tour, naver);

            String aiText = geminiApiClient.generateText(prompt, model -> {
                emit(sink, TravelEvent.builder()
                        .type("model").stage("generating")
                        .message("AI 모델을 선택했습니다: " + model)
                        .payload(Map.of("model", model))
                        .build());
            }).block();

            if (aiText == null || aiText.isBlank()) {
                aiText = "AI가 응답을 반환하지 않았습니다.";
            }

            // 프론트에서 live text 를 보여주기 위해 결과 텍스트를 청크로 쪼개 송출.
            for (String chunk : chunk(aiText, 60)) {
                emit(sink, TravelEvent.builder()
                        .type("token").stage("generating")
                        .payload(chunk)
                        .build());
            }

            TravelPlanDto plan = tryParsePlan(aiText);
            if (plan != null && plan.getItinerary() != null && !plan.getItinerary().isEmpty()) {
                emit(sink, TravelEvent.builder()
                        .type("plan").stage("completed")
                        .message(String.format("%s · %s 일정이 완성됐어요.",
                                plan.getDestination() != null ? plan.getDestination() : "여행지",
                                plan.getDuration() != null ? plan.getDuration() : "맞춤"))
                        .payload(plan)
                        .build());
            } else {
                emit(sink, TravelEvent.builder()
                        .type("plan").stage("completed")
                        .message("구조화(JSON) 파싱 결과가 없어 텍스트 본문을 그대로 사용합니다.")
                        .payload(Map.of("fallback", true, "text", aiText))
                        .build());
            }

            emit(sink, TravelEvent.builder()
                    .type("done").stage("completed")
                    .message("여행 계획 생성이 완료되었습니다.")
                    .build());

            sink.tryEmitComplete();
        } catch (Exception e) {
            log.error("[orchestrator] 파이프라인 실패", e);
            emit(sink, TravelEvent.builder()
                    .type("error").stage("error")
                    .message("여행 계획 생성 중 오류가 발생했습니다: " + e.getMessage())
                    .build());
            sink.tryEmitComplete();
        }
    }

    private void emit(Sinks.Many<TravelEvent> sink, TravelEvent event) {
        Sinks.EmitResult r = sink.tryEmitNext(event);
        if (r.isFailure()) {
            log.debug("[orchestrator] emit 실패({}): {}", r, event.getType());
        }
    }

    private String describeIntent(IntentAnalysisDto intent) {
        if (intent == null) return "의도 분석 결과 없음";
        String area = intent.getArea() != null ? intent.getArea().getName() : "미지정";
        String keyword = intent.getKeyword() != null ? intent.getKeyword() : "없음";
        return String.format("의도 분석 완료 — 분류: %s · 지역: %s · 키워드: %s",
                intent.getIntent(), area, keyword);
    }

    private TouristSpotResponseDto safeCollectTour(IntentAnalysisDto intent) {
        try {
            if (intent == null || intent.getArea() == null) {
                return touristInfoService.getRandomTouristSpots(1, 20);
            }
            String areaCode = intent.getArea().getCode();
            String sigunguCode = intent.getArea().getSigungu() != null
                    ? intent.getArea().getSigungu().getCode()
                    : null;
            if (sigunguCode == null || "UNKNOWN".equals(sigunguCode)) {
                return touristInfoService.getRandomSigunguTouristSpots(areaCode, 1, 20);
            }
            return touristInfoService.getTouristSpots(areaCode, sigunguCode, 1, 20);
        } catch (Exception e) {
            log.warn("[orchestrator] 관광공사 API 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    private NaverBlogSearchDto safeCollectNaver(String query) {
        try {
            return naverSearchService.searchBlog(query).block();
        } catch (Exception e) {
            log.warn("[orchestrator] 네이버 검색 실패 (무시): {}", e.getMessage());
            return null;
        }
    }

    private SourceSummaryDto toTourSummary(IntentAnalysisDto intent, TouristSpotResponseDto tour) {
        if (tour == null || tour.getItems() == null) {
            return SourceSummaryDto.builder()
                    .source("tour").count(0).items(new ArrayList<>())
                    .context(intent != null && intent.getArea() != null ? intent.getArea().getName() : null)
                    .build();
        }
        List<SourceSummaryDto.SourceItem> items = tour.getItems().stream()
                .limit(8)
                .map(this::toTourItem)
                .collect(Collectors.toList());
        String context = null;
        if (!tour.getItems().isEmpty()) {
            TouristSpotDto first = tour.getItems().get(0);
            context = String.format("%s %s",
                    nullSafe(first.getAreaNm()),
                    nullSafe(first.getSignguNm())).trim();
        }
        return SourceSummaryDto.builder()
                .source("tour")
                .count(nullSafe(tour.getItemCount()))
                .items(items)
                .context(context)
                .build();
    }

    private SourceSummaryDto.SourceItem toTourItem(TouristSpotDto spot) {
        return SourceSummaryDto.SourceItem.builder()
                .title(nullSafe(spot.getHubTatsNm()))
                .subtitle(String.format("%s · %s",
                        nullSafe(spot.getSignguNm()),
                        nullSafe(spot.getHubCtgryMclsNm())))
                .build();
    }

    private SourceSummaryDto toNaverSummary(String query, NaverBlogSearchDto naver) {
        if (naver == null || naver.getItems() == null) {
            return SourceSummaryDto.builder()
                    .source("naver").count(0).items(new ArrayList<>())
                    .context(query)
                    .build();
        }
        List<SourceSummaryDto.SourceItem> items = naver.getItems().stream()
                .limit(5)
                .map(item -> SourceSummaryDto.SourceItem.builder()
                        .title(stripHtml(item.getTitle()))
                        .subtitle(nullSafe(item.getBloggerName()))
                        .url(item.getLink())
                        .build())
                .collect(Collectors.toList());
        return SourceSummaryDto.builder()
                .source("naver")
                .count(naver.getTotal() != null ? naver.getTotal() : naver.getItems().size())
                .items(items)
                .context(query)
                .build();
    }

    private String buildStructuredPrompt(IntentAnalysisDto intent, TouristSpotResponseDto tour,
                                         NaverBlogSearchDto naver) {
        StringBuilder ctx = new StringBuilder();
        if (tour != null && tour.getItems() != null && !tour.getItems().isEmpty()) {
            ctx.append("=== 관광공사 API 관광지 목록 ===\n");
            tour.getItems().stream().limit(12).forEach(s -> ctx.append(String.format(
                    "- %s (%s %s · %s)\n",
                    nullSafe(s.getHubTatsNm()),
                    nullSafe(s.getAreaNm()),
                    nullSafe(s.getSignguNm()),
                    nullSafe(s.getHubCtgryMclsNm()))));
            ctx.append("\n");
        }
        if (naver != null && naver.getItems() != null && !naver.getItems().isEmpty()) {
            ctx.append("=== 네이버 블로그 참고 ===\n");
            naver.getItems().stream().limit(5).forEach(b -> ctx.append(String.format(
                    "- %s — %s\n  %s\n",
                    stripHtml(b.getTitle()),
                    nullSafe(b.getBloggerName()),
                    stripHtml(b.getDescription()))));
            ctx.append("\n");
        }
        if (ctx.length() == 0) {
            ctx.append("참고 정보가 없습니다. 일반적인 여행 상식으로 생성해 주세요.\n");
        }

        String area = intent != null && intent.getArea() != null ? intent.getArea().getName() : "미지정";
        String keyword = intent != null && intent.getKeyword() != null ? intent.getKeyword() : "없음";
        String intentLine = String.format("의도=%s, 지역=%s, 키워드=%s, 블로그 후보=%d건",
                intent != null ? intent.getIntent() : "general",
                area, keyword,
                naver != null && naver.getItems() != null ? naver.getItems().size() : 0);

        Map<String, String> variables = new HashMap<>();
        variables.put("intent", intentLine);
        variables.put("context", ctx.toString());

        String prompt = promptLoader.getPromptWithVariables("travel_plan_structured", variables);
        if (prompt == null) {
            // 프롬프트 템플릿이 없으면 간단 프롬프트 폴백
            prompt = "아래 참고 정보를 바탕으로 여행 계획을 JSON 으로 작성해줘.\n\n"
                    + "의도: " + intentLine + "\n\n"
                    + "참고 정보:\n" + ctx;
        }
        return prompt;
    }

    private TravelPlanDto tryParsePlan(String text) {
        try {
            String json = extractJson(text);
            if (json == null || json.isBlank() || !json.startsWith("{")) return null;
            return objectMapper.readValue(json, TravelPlanDto.class);
        } catch (Exception e) {
            log.warn("[orchestrator] 구조화 플랜 파싱 실패 (텍스트로 대체): {}", e.getMessage());
            return null;
        }
    }

    private String extractJson(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.startsWith("```")) {
            int first = t.indexOf('\n');
            if (first > 0) t = t.substring(first + 1);
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
            t = t.trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return null;
    }

    private List<String> chunk(String s, int size) {
        List<String> out = new ArrayList<>();
        if (s == null) return out;
        int n = s.length();
        for (int i = 0; i < n; i += size) {
            out.add(s.substring(i, Math.min(n, i + size)));
        }
        return out;
    }

    private String stripHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", "").replace("&quot;", "\"").replace("&amp;", "&");
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private int nullSafe(Integer i) {
        return i == null ? 0 : i;
    }
}
