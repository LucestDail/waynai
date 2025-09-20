package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.IntentAnalysisWithSearchDto;
import com.waynai.demo.util.AreaCodeUtil;
import com.waynai.demo.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 의도 분석 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentAnalysisService {

    private final GeminiApiClient geminiApiClient;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final AreaCodeUtil areaCodeUtil;
    private final NaverSearchService naverSearchService;

    /**
     * 사용자 입력 의도 분석
     * @param query 사용자 입력
     * @return 의도 분석 결과
     */
    public Mono<IntentAnalysisDto> analyzeIntent(String query) {
        try {
            log.info("의도 분석 시작: {}", query);
            
            String prompt = promptLoader.getPrompt("intent_analysis");
            if (prompt == null) {
                throw new RuntimeException("의도 분석 프롬프트를 찾을 수 없습니다.");
            }
            
            // 지역 데이터를 문자열로 변환
            String areaData = formatAreaDataForPrompt();
            
            // 프롬프트에 쿼리와 지역 데이터 치환
            String fullPrompt = prompt.replace("$query", query).replace("$areaData", areaData);
            
            return geminiApiClient.generateText(fullPrompt)
                    .map(response -> {
                        try {
                            // API 오류 응답인지 확인
                            if (response.contains("\"error\"") && response.contains("\"status\"")) {
                                log.warn("API 오류 응답 감지: {}", response);
                                // 기본 의도 분석 결과 반환
                                IntentAnalysisDto defaultResult = new IntentAnalysisDto();
                                defaultResult.setIntent("general");
                                defaultResult.setConfidence(0.5);
                                defaultResult.setReason("AI 서비스 일시적 오류로 인한 기본 응답");
                                return defaultResult;
                            }
                            
                            // JSON 코드 블록 제거
                            String cleanResponse = response;
                            if (cleanResponse.startsWith("```json")) {
                                cleanResponse = cleanResponse.replaceAll("^```json\\n", "").replaceAll("\\n```$", "");
                            } else if (cleanResponse.startsWith("```")) {
                                cleanResponse = cleanResponse.replaceAll("^```\\n", "").replaceAll("\\n```$", "");
                            }
                            
                            IntentAnalysisDto result = objectMapper.readValue(cleanResponse, IntentAnalysisDto.class);
                            log.info("의도 분석 완료: {}", result);
                            return result;
                        } catch (Exception e) {
                            log.error("의도 분석 결과 파싱 실패", e);
                            // 파싱 실패 시 기본 의도 분석 결과 반환
                            IntentAnalysisDto defaultResult = new IntentAnalysisDto();
                            defaultResult.setIntent("general");
                            defaultResult.setConfidence(0.5);
                            defaultResult.setReason("응답 파싱 오류로 인한 기본 응답");
                            return defaultResult;
                        }
                    })
                    .doOnError(error -> log.error("의도 분석 실패", error));
                    
        } catch (Exception e) {
            log.error("의도 분석 서비스 오류", e);
            return Mono.error(new RuntimeException("의도 분석 서비스 오류: " + e.getMessage()));
        }
    }
    
    /**
     * 프롬프트용 지역 데이터 포맷팅
     * @return 포맷된 지역 데이터 문자열
     */
    private String formatAreaDataForPrompt() {
        try {
            var areaCodes = areaCodeUtil.getAllAreaCodes();
            
            // 지역별로 그룹화하여 정리
            StringBuilder areaData = new StringBuilder();
            String currentArea = "";
            
            for (var areaCode : areaCodes) {
                if (!currentArea.equals(areaCode.getAreaName())) {
                    if (!currentArea.isEmpty()) {
                        areaData.append("\n");
                    }
                    currentArea = areaCode.getAreaName();
                    areaData.append(String.format("지역: %s (코드: %s)\n", 
                        areaCode.getAreaName(), areaCode.getAreaCode()));
                }
                areaData.append(String.format("  - %s (코드: %s)\n", 
                    areaCode.getSigunguName(), areaCode.getSigunguCode()));
            }
            
            return areaData.toString();
        } catch (Exception e) {
            log.warn("지역 데이터 포맷팅 실패", e);
            return "지역 데이터를 불러올 수 없습니다.";
        }
    }

    /**
     * 사용자 입력 의도 분석 및 네이버 검색 (None인 경우)
     * @param query 사용자 입력
     * @return 의도 분석 결과와 네이버 검색 결과
     */
    public Mono<IntentAnalysisWithSearchDto> analyzeIntentWithSearch(String query) {
        return analyzeIntent(query)
                .flatMap(intentAnalysis -> {
                    // 의도가 "none"이거나 "general"인 경우 네이버 검색 수행
                    if ("none".equals(intentAnalysis.getIntent()) || "general".equals(intentAnalysis.getIntent())) {
                        log.info("의도 분석 결과가 'none' 또는 'general'이므로 네이버 검색 수행: {}", query);
                        return naverSearchService.searchBlog(query)
                                .map(naverResult -> IntentAnalysisWithSearchDto.builder()
                                        .intentAnalysis(intentAnalysis)
                                        .naverSearchResult(naverResult)
                                        .hasNaverSearch(true)
                                        .build())
                                .onErrorResume(error -> {
                                    log.error("네이버 검색 실패", error);
                                    return Mono.just(IntentAnalysisWithSearchDto.builder()
                                            .intentAnalysis(intentAnalysis)
                                            .naverSearchResult(null)
                                            .hasNaverSearch(false)
                                            .build());
                                });
                    } else {
                        // 의도가 "none"이 아닌 경우 네이버 검색 없이 반환
                        return Mono.just(IntentAnalysisWithSearchDto.builder()
                                .intentAnalysis(intentAnalysis)
                                .naverSearchResult(null)
                                .hasNaverSearch(false)
                                .build());
                    }
                })
                .onErrorResume(error -> {
                    log.error("의도 분석 실패, 네이버 검색으로 대체: {}", query, error);
                    // 의도 분석 실패 시 네이버 검색 수행
                    return naverSearchService.searchBlog(query)
                            .map(naverResult -> {
                                // 기본 의도 분석 결과 생성
                                IntentAnalysisDto defaultIntent = IntentAnalysisDto.builder()
                                        .intent("none")
                                        .confidence(0.5)
                                        .reason("의도 분석 실패로 인한 네이버 검색 대체")
                                        .build();
                                
                                return IntentAnalysisWithSearchDto.builder()
                                        .intentAnalysis(defaultIntent)
                                        .naverSearchResult(naverResult)
                                        .hasNaverSearch(true)
                                        .build();
                            })
                            .onErrorResume(naverError -> {
                                log.error("네이버 검색도 실패", naverError);
                                IntentAnalysisDto defaultIntent = IntentAnalysisDto.builder()
                                        .intent("none")
                                        .confidence(0.0)
                                        .reason("의도 분석 및 네이버 검색 모두 실패")
                                        .build();
                                
                                return Mono.just(IntentAnalysisWithSearchDto.builder()
                                        .intentAnalysis(defaultIntent)
                                        .naverSearchResult(null)
                                        .hasNaverSearch(false)
                                        .build());
                            });
                })
                .onErrorResume(error -> {
                    log.error("전체 프로세스 실패, 네이버 검색으로 대체: {}", query, error);
                    // 전체 프로세스 실패 시에도 네이버 검색 수행
                    return naverSearchService.searchBlog(query)
                            .map(naverResult -> {
                                IntentAnalysisDto defaultIntent = IntentAnalysisDto.builder()
                                        .intent("none")
                                        .confidence(0.5)
                                        .reason("전체 프로세스 실패로 인한 네이버 검색 대체")
                                        .build();
                                
                                return IntentAnalysisWithSearchDto.builder()
                                        .intentAnalysis(defaultIntent)
                                        .naverSearchResult(naverResult)
                                        .hasNaverSearch(true)
                                        .build();
                            })
                            .onErrorResume(naverError -> {
                                log.error("네이버 검색도 실패", naverError);
                                IntentAnalysisDto defaultIntent = IntentAnalysisDto.builder()
                                        .intent("none")
                                        .confidence(0.0)
                                        .reason("전체 프로세스 및 네이버 검색 모두 실패")
                                        .build();
                                
                                return Mono.just(IntentAnalysisWithSearchDto.builder()
                                        .intentAnalysis(defaultIntent)
                                        .naverSearchResult(null)
                                        .hasNaverSearch(false)
                                        .build());
                            });
                });
    }
}
