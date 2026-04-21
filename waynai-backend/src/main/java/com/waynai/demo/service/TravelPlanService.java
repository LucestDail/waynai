package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.IntentAnalysisWithSearchDto;
import com.waynai.demo.dto.NaverBlogSearchDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 여행 계획 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private final GeminiApiClient geminiApiClient;
    private final PromptLoader promptLoader;
    private final TouristInfoService touristInfoService;
    private final RelatedTouristInfoService relatedTouristInfoService;
    private final IntentAnalysisService intentAnalysisService;
    private final ObjectMapper objectMapper;

    /**
     * 구조화된 여행 계획 생성. Gemini 에 JSON 스키마 프롬프트를 전달하고,
     * 응답에서 JSON 블록을 파싱하여 {@link TravelPlanDto} 로 반환합니다.
     * 네이버 블로그 RAG 컨텍스트를 함께 활용합니다.
     */
    public Mono<TravelPlanDto> generateStructuredPlan(String query) {
        return intentAnalysisService.analyzeIntentWithSearch(query)
                .flatMap(result -> {
                    String context = collectContextWithSearch(result);
                    String intentInfo = formatIntent(result.getIntentAnalysis(),
                            result.isHasNaverSearch() && result.getNaverSearchResult() != null
                                    ? result.getNaverSearchResult().getTotal() : 0);
                    Map<String, String> variables = new HashMap<>();
                    variables.put("intent", intentInfo);
                    variables.put("context", context);
                    String prompt = promptLoader.getPromptWithVariables("travel_plan_structured", variables);
                    if (prompt == null) {
                        log.warn("structured 프롬프트 로드 실패. 텍스트 플랜으로 폴백.");
                        return fallbackTextPlan(result, "structured 프롬프트를 찾지 못했습니다.");
                    }
                    return geminiApiClient.generateText(prompt)
                            .map(this::extractJson)
                            .flatMap(json -> {
                                try {
                                    TravelPlanDto dto = objectMapper.readValue(json, TravelPlanDto.class);
                                    if (dto.getItinerary() == null || dto.getItinerary().isEmpty()) {
                                        log.warn("구조화 응답의 itinerary 가 비어 있음. 텍스트 플랜으로 폴백.");
                                        return fallbackTextPlan(result, "AI가 itinerary 없이 응답했습니다.");
                                    }
                                    return Mono.just(dto);
                                } catch (Exception e) {
                                    log.warn("구조화 JSON 파싱 실패 → 텍스트 플랜 폴백: {}", e.getMessage());
                                    return fallbackTextPlan(result, "JSON 파싱 실패: " + e.getMessage());
                                }
                            })
                            .onErrorResume(e -> {
                                log.warn("구조화 플랜 생성 실패 → 텍스트 플랜 폴백: {}", e.getMessage());
                                return fallbackTextPlan(result, "구조화 생성 실패: " + e.getMessage());
                            });
                })
                .onErrorResume(e -> {
                    log.error("intent 분석 자체 실패. 최소 폴백 DTO 반환: {}", e.getMessage());
                    TravelPlanDto dto = TravelPlanDto.builder()
                            .type("travel_plan")
                            .destination("알 수 없음")
                            .summary("여행 계획 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.")
                            .itinerary(new ArrayList<>())
                            .warnings(new ArrayList<>(List.of("의도 분석 실패: " + e.getMessage())))
                            .build();
                    return Mono.just(dto);
                });
    }

    /**
     * 구조화 엔드포인트가 실패하거나 itinerary 가 비었을 때, 텍스트 스트림 결과를
     * 하나의 문자열로 합쳐 summary 에 담아 반환한다. 프론트에서는 summary 를 그대로
     * 렌더링하여 사용자가 결과를 볼 수 있도록 한다.
     */
    private Mono<TravelPlanDto> fallbackTextPlan(IntentAnalysisWithSearchDto result, String reason) {
        return generateTravelPlanWithSearch(result)
                .collectList()
                .map(chunks -> {
                    String merged = String.join("", chunks).trim();
                    IntentAnalysisDto intent = result.getIntentAnalysis();
                    String destination = (intent != null && intent.getArea() != null)
                            ? intent.getArea().getName() : "여행지";
                    List<String> warnings = new ArrayList<>();
                    warnings.add("구조화(JSON) 응답 생성에 실패하여 텍스트 결과로 대체되었습니다.");
                    if (reason != null && !reason.isBlank()) {
                        warnings.add("사유: " + reason);
                    }
                    return TravelPlanDto.builder()
                            .type("travel_plan")
                            .destination(destination)
                            .summary(merged.isEmpty()
                                    ? "AI가 응답을 반환하지 않았습니다. 다시 시도해 주세요."
                                    : merged)
                            .itinerary(new ArrayList<>())
                            .warnings(warnings)
                            .build();
                })
                .onErrorResume(e -> {
                    log.error("텍스트 폴백 자체 실패: {}", e.getMessage());
                    TravelPlanDto dto = TravelPlanDto.builder()
                            .type("travel_plan")
                            .destination("알 수 없음")
                            .summary("여행 계획 생성에 실패했습니다.")
                            .itinerary(new ArrayList<>())
                            .warnings(new ArrayList<>(List.of(
                                    "구조화/텍스트 모두 실패: " + e.getMessage(),
                                    reason != null ? reason : "원인 불명"
                            )))
                            .build();
                    return Mono.just(dto);
                });
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
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
        return t;
    }

    private String formatIntent(IntentAnalysisDto intent, int naverCount) {
        String area = intent != null && intent.getArea() != null ? intent.getArea().getName() : "없음";
        String keyword = intent != null && intent.getKeyword() != null ? intent.getKeyword() : "없음";
        String intentName = intent != null ? String.valueOf(intent.getIntent()) : "GENERAL";
        return String.format("의도=%s, 지역=%s, 키워드=%s, 블로그 후보=%d건",
                intentName, area, keyword, naverCount);
    }

    /**
     * 여행 계획 생성 (네이버 검색 결과 포함)
     * @param result 의도 분석 및 네이버 검색 결과
     * @return 여행 계획 스트림
     */
    public Flux<String> generateTravelPlanWithSearch(IntentAnalysisWithSearchDto result) {
        try {
            log.info("여행 계획 생성 시작 (네이버 검색 포함): {}", result);
            
            // 1. 컨텍스트 정보 수집
            String context = collectContextWithSearch(result);
            
            // 2. 프롬프트 생성
            String prompt = createTravelPlanPromptWithSearch(result, context);
            
            // 프롬프트와 연관 데이터 로깅
            log.info("=== 여행 계획 생성 프롬프트 (네이버 검색 포함) ===");
            log.info("의도 분석 결과: {}", result.getIntentAnalysis());
            log.info("네이버 검색 결과 있음: {}", result.isHasNaverSearch());
            log.info("수집된 컨텍스트: {}", context.toString());
            log.info("생성된 프롬프트 길이: {} 문자", prompt.length());
            log.info("프롬프트 내용: {}", prompt);
            log.info("=== 프롬프트 끝 ===");
            
            // 3. Gemini API 호출하여 여행 계획 생성
            return geminiApiClient.generateTextStream(prompt)
                    .doOnComplete(() -> log.info("여행 계획 생성 완료 (네이버 검색 포함)"))
                    .doOnError(error -> log.error("여행 계획 생성 오류 (네이버 검색 포함)", error));
                    
        } catch (Exception e) {
            log.error("여행 계획 서비스 오류 (네이버 검색 포함)", e);
            return Flux.just("죄송합니다. 여행 계획 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 여행 계획 생성
     * @param intentAnalysis 의도 분석 결과
     * @return 여행 계획 스트림
     */
    public Flux<String> generateTravelPlan(IntentAnalysisDto intentAnalysis) {
        try {
            log.info("여행 계획 생성 시작: {}", intentAnalysis);
            
            // 1. 컨텍스트 정보 수집
            String context = collectContext(intentAnalysis);
            
            // 2. 프롬프트 생성
            String prompt = createTravelPlanPrompt(intentAnalysis, context);
            
            // 프롬프트와 연관 데이터 로깅
            log.info("=== 여행 계획 생성 프롬프트 ===");
            log.info("의도 분석 결과: {}", intentAnalysis);
            log.info("수집된 컨텍스트: {}", context.toString());
            log.info("생성된 프롬프트 길이: {} 문자", prompt.length());
            log.info("프롬프트 내용: {}", prompt);
            log.info("=== 프롬프트 끝 ===");
            
            // 3. Gemini API 호출하여 여행 계획 생성
            return geminiApiClient.generateTextStream(prompt)
                    .doOnComplete(() -> log.info("여행 계획 생성 완료"))
                    .doOnError(error -> log.error("여행 계획 생성 오류", error));
                    
        } catch (Exception e) {
            log.error("여행 계획 서비스 오류", e);
            return Flux.just("죄송합니다. 여행 계획 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 컨텍스트 정보 수집
     * @param intentAnalysis 의도 분석 결과
     * @return 컨텍스트 문자열
     */
    private String collectContext(IntentAnalysisDto intentAnalysis) {
        StringBuilder context = new StringBuilder();
        
        try {
            // 지역 기반 관광지 정보 수집
            if (intentAnalysis.getArea() != null) {
                String areaCode = intentAnalysis.getArea().getCode();
                String sigunguCode = intentAnalysis.getArea().getSigungu() != null 
                    ? intentAnalysis.getArea().getSigungu().getCode() 
                    : null;
                
                // UNKNOWN 시군구 코드 처리
                if (sigunguCode != null && !"UNKNOWN".equals(sigunguCode)) {
                    var touristResponse = touristInfoService.getTouristSpots(areaCode, sigunguCode, 1, 20);
                    if (touristResponse.isSuccess() && touristResponse.getItems() != null) {
                        context.append("=== 지역 관광지 정보 ===\n");
                        context.append(formatTouristSpotsContext(touristResponse.getItems()));
                        context.append("\n\n");
                    }
                } else if (sigunguCode == null || "UNKNOWN".equals(sigunguCode)) {
                    // 시군구가 없거나 UNKNOWN인 경우 해당 지역의 무작위 관광지 조회
                    log.info("시군구 정보가 없거나 UNKNOWN이므로 무작위 관광지 조회: areaCode={}", areaCode);
                    var touristResponse = touristInfoService.getRandomTouristSpots(1, 20);
                    if (touristResponse.isSuccess() && touristResponse.getItems() != null) {
                        context.append("=== 지역 관광지 정보 (무작위) ===\n");
                        context.append(formatTouristSpotsContext(touristResponse.getItems()));
                        context.append("\n\n");
                    }
                }
            }
            
            // 키워드 기반 연관 관광지 정보 수집
            if (intentAnalysis.getKeyword() != null && !intentAnalysis.getKeyword().trim().isEmpty()) {
                String areaCode = intentAnalysis.getArea() != null ? intentAnalysis.getArea().getCode() : "11";
                String sigunguCode = intentAnalysis.getArea() != null && intentAnalysis.getArea().getSigungu() != null 
                    ? intentAnalysis.getArea().getSigungu().getCode() 
                    : "11530";
                
                // UNKNOWN 시군구 코드 처리
                if ("UNKNOWN".equals(sigunguCode)) {
                    // UNKNOWN인 경우 기본 시군구 사용
                    sigunguCode = "11530"; // 서울 강남구
                    log.info("UNKNOWN 시군구 코드를 기본값으로 변경: {}", sigunguCode);
                }
                
                var relatedResponse = relatedTouristInfoService.searchKeywordRelatedSpots(
                    areaCode, sigunguCode, intentAnalysis.getKeyword(), 1, 20);
                
                if (relatedResponse.isSuccess() && relatedResponse.getItems() != null) {
                    context.append("=== 키워드 기반 연관 관광지 정보 ===\n");
                    context.append(formatRelatedTouristSpotsContext(relatedResponse.getItems()));
                    context.append("\n\n");
                }
            }
            
            // 컨텍스트가 없으면 기본 메시지
            if (context.length() == 0) {
                context.append("관광지 정보가 없습니다.");
            }
            
        } catch (Exception e) {
            log.warn("컨텍스트 수집 중 오류 발생", e);
            context.append("관광지 정보 수집 중 오류가 발생했습니다.");
        }
        
        return context.toString();
    }

    /**
     * 컨텍스트 정보 수집 (네이버 검색 결과 포함)
     * @param result 의도 분석 및 네이버 검색 결과
     * @return 컨텍스트 문자열
     */
    private String collectContextWithSearch(IntentAnalysisWithSearchDto result) {
        StringBuilder context = new StringBuilder();
        
        try {
            IntentAnalysisDto intentAnalysis = result.getIntentAnalysis();
            
            // 네이버 검색 결과가 있는 경우 추가
            if (result.isHasNaverSearch() && result.getNaverSearchResult() != null) {
                context.append("=== 네이버 블로그 검색 결과 ===\n");
                context.append(formatNaverSearchContext(result.getNaverSearchResult()));
                context.append("\n\n");
                
                // 네이버 검색 결과가 있으면 지역 기반 관광지 정보는 수집하지 않음
                // (네이버 검색 결과가 더 관련성이 높으므로)
                context.append("관광지 정보: 네이버 검색 결과를 참고하여 여행 계획을 수립하세요.");
            } else {
                // 네이버 검색 결과가 없는 경우에만 기존 컨텍스트 수집 로직 사용
                String existingContext = collectContext(intentAnalysis);
                context.append(existingContext);
            }
            
        } catch (Exception e) {
            log.warn("컨텍스트 수집 중 오류 발생 (네이버 검색 포함)", e);
            context.append("관광지 정보 수집 중 오류가 발생했습니다.");
        }
        
        return context.toString();
    }

    /**
     * 여행 계획 프롬프트 생성
     * @param intentAnalysis 의도 분석 결과
     * @param context 컨텍스트
     * @return 완성된 프롬프트
     */
    private String createTravelPlanPrompt(IntentAnalysisDto intentAnalysis, String context) {
        String prompt = promptLoader.getPrompt("travel_plan");
        if (prompt == null) {
            throw new RuntimeException("여행 계획 프롬프트를 찾을 수 없습니다.");
        }

        // 의도 정보를 문자열로 변환
        String intentInfo = String.format(
            "의도: %s, 키워드: %s, 지역: %s",
            intentAnalysis.getIntent(),
            intentAnalysis.getKeyword() != null ? intentAnalysis.getKeyword() : "없음",
            intentAnalysis.getArea() != null ? intentAnalysis.getArea().getName() : "없음"
        );

        Map<String, String> variables = new HashMap<>();
        variables.put("intent", intentInfo);
        variables.put("context", context);

        return promptLoader.getPromptWithVariables("travel_plan", variables);
    }

    /**
     * 여행 계획 프롬프트 생성 (네이버 검색 결과 포함)
     * @param result 의도 분석 및 네이버 검색 결과
     * @param context 컨텍스트
     * @return 완성된 프롬프트
     */
    private String createTravelPlanPromptWithSearch(IntentAnalysisWithSearchDto result, String context) {
        String prompt = promptLoader.getPrompt("travel_plan");
        if (prompt == null) {
            throw new RuntimeException("여행 계획 프롬프트를 찾을 수 없습니다.");
        }

        IntentAnalysisDto intentAnalysis = result.getIntentAnalysis();
        
        // 의도 정보를 문자열로 변환
        String intentInfo = String.format(
            "의도: %s, 키워드: %s, 지역: %s",
            intentAnalysis.getIntent(),
            intentAnalysis.getKeyword() != null ? intentAnalysis.getKeyword() : "없음",
            intentAnalysis.getArea() != null ? intentAnalysis.getArea().getName() : "없음"
        );

        // 네이버 검색 결과가 있는 경우 추가 정보 포함
        if (result.isHasNaverSearch() && result.getNaverSearchResult() != null) {
            intentInfo += String.format(", 네이버 검색 결과: %d개", result.getNaverSearchResult().getTotal());
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("intent", intentInfo);
        variables.put("context", context);

        return promptLoader.getPromptWithVariables("travel_plan", variables);
    }

    /**
     * 네이버 검색 결과를 컨텍스트 텍스트로 변환
     * @param searchResult 네이버 검색 결과
     * @return 컨텍스트 텍스트
     */
    private String formatNaverSearchContext(NaverBlogSearchDto searchResult) {
        if (searchResult == null || searchResult.getItems() == null || searchResult.getItems().isEmpty()) {
            return "네이버 검색 결과가 없습니다.";
        }

        StringBuilder context = new StringBuilder();
        context.append(String.format("총 검색 결과: %d개\n", searchResult.getTotal()));
        context.append("주요 검색 결과:\n\n");

        // 상위 5개 결과만 포함
        searchResult.getItems().stream()
                .limit(5)
                .forEach(item -> {
                    context.append(String.format("제목: %s\n", item.getTitle()));
                    context.append(String.format("설명: %s\n", item.getDescription()));
                    context.append(String.format("블로거: %s\n", item.getBloggerName()));
                    context.append(String.format("작성일: %s\n", item.getPostDate()));
                    context.append(String.format("링크: %s\n", item.getLink()));
                    context.append("---\n");
                });

        return context.toString();
    }

    /**
     * 관광지 정보를 컨텍스트 텍스트로 변환
     * @param spots 관광지 목록
     * @return 컨텍스트 텍스트
     */
    private String formatTouristSpotsContext(List<com.waynai.demo.dto.TouristSpotDto> spots) {
        if (spots == null || spots.isEmpty()) {
            return "관광지 정보가 없습니다.";
        }

        return spots.stream()
                .map(spot -> String.format(
                    "관광지명: %s\n" +
                    "지역: %s %s\n" +
                    "시군구: %s %s\n" +
                    "대분류: %s\n" +
                    "중분류: %s\n" +
                    "소분류: %s\n" +
                    "순위: %s\n" +
                    "---",
                    spot.getHubTatsNm(),
                    spot.getAreaNm(), spot.getAreaCd(),
                    spot.getSignguNm(), spot.getSignguCd(),
                    spot.getHubCtgryLclsNm(),
                    spot.getHubCtgryMclsNm(),
                    spot.getHubCtgrySclsNm() != null ? spot.getHubCtgrySclsNm() : "정보없음",
                    spot.getHubRank()
                ))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("관광지 정보가 없습니다.");
    }

    /**
     * 연관 관광지 정보를 컨텍스트 텍스트로 변환
     * @param spots 연관 관광지 목록
     * @return 컨텍스트 텍스트
     */
    private String formatRelatedTouristSpotsContext(List<com.waynai.demo.dto.RelatedTouristSpotDto> spots) {
        if (spots == null || spots.isEmpty()) {
            return "연관 관광지 정보가 없습니다.";
        }

        return spots.stream()
                .map(spot -> String.format(
                    "기준 관광지: %s\n" +
                    "연관 관광지: %s\n" +
                    "연관 지역: %s %s\n" +
                    "연관 시군구: %s %s\n" +
                    "연관 대분류: %s\n" +
                    "연관 중분류: %s\n" +
                    "연관 소분류: %s\n" +
                    "---",
                    spot.getTAtsNm(),
                    spot.getRlteTatsNm(),
                    spot.getRlteRegnNm(), spot.getRlteRegnCd(),
                    spot.getRlteSignguNm(), spot.getRlteSignguCd(),
                    spot.getRlteCtgryLclsNm(),
                    spot.getRlteCtgryMclsNm(),
                    spot.getRlteCtgrySclsNm()
                ))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("연관 관광지 정보가 없습니다.");
    }
}
