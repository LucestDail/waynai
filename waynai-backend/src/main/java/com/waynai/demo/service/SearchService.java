package com.waynai.demo.service;

import com.waynai.demo.dto.SearchRequestDto;
import com.waynai.demo.dto.SearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    
    private final LLMService llmService;
    private final TouristInfoService touristInfoService;
    
    public Flux<SearchResponseDto> processSearch(SearchRequestDto request) {
        return Flux.create(sink -> {
            try {
                List<String> progress = new ArrayList<>();
                
                // 1. 의도 분석 단계
                progress.add("사용자 입력 분석 중...");
                sink.next(createResponse("analyzing", "의도 분석 중", "intent_analysis", null, new ArrayList<>(progress)));
                
                String intent = analyzeIntent(request.getQuery());
                progress.add("의도 분석 완료: " + intent);
                sink.next(createResponse("analyzing", "의도 분석 완료", "intent_analysis", intent, new ArrayList<>(progress)));
                
                Thread.sleep(1000); // 시각적 효과를 위한 지연
                
                // 2. 질의 강화 단계
                progress.add("질의 강화 중...");
                sink.next(createResponse("enhancing", "질의 강화 중", "query_enhancement", null, new ArrayList<>(progress)));
                
                String enhancedQuery = enhanceQuery(request, intent);
                progress.add("질의 강화 완료: " + enhancedQuery);
                sink.next(createResponse("enhancing", "질의 강화 완료", "query_enhancement", enhancedQuery, new ArrayList<>(progress)));
                
                Thread.sleep(1000);
                
                // 3. 내부 지식 검색 단계
                progress.add("관광지 정보 검색 중...");
                sink.next(createResponse("searching", "관광지 정보 검색 중", "knowledge_search", null, new ArrayList<>(progress)));
                
                String touristContext = searchTouristInfo(request, enhancedQuery);
                progress.add("관광지 정보 검색 완료");
                sink.next(createResponse("searching", "관광지 정보 검색 완료", "knowledge_search", touristContext, new ArrayList<>(progress)));
                
                Thread.sleep(1000);
                
                // 4. 코스 생성 단계
                progress.add("여행 코스 생성 중...");
                sink.next(createResponse("generating", "여행 코스 생성 중", "course_generation", null, new ArrayList<>(progress)));
                
                Object result = generateResult(request, enhancedQuery, touristContext);
                progress.add("여행 코스 생성 완료");
                sink.next(createResponse("completed", "검색 완료", "course_generation", result, new ArrayList<>(progress)));
                
                sink.complete();
                
            } catch (Exception e) {
                log.error("Search processing error: {}", e.getMessage(), e);
                List<String> errorProgress = new ArrayList<>();
                errorProgress.add("오류 발생: " + e.getMessage());
                sink.next(createResponse("error", "처리 중 오류 발생", "error", e.getMessage(), errorProgress));
                sink.complete();
            }
        });
    }
    
    private String analyzeIntent(String query) {
        // LLM을 사용하여 의도 분석
        String prompt = String.format(
            "다음 사용자 입력의 의도를 분석해주세요:\n" +
            "입력: %s\n\n" +
            "분석 결과를 다음 중 하나로 분류해주세요:\n" +
            "1. KEYWORD: 단순 키워드나 관광지명\n" +
            "2. SENTENCE: 여행 계획이나 문장 형태의 요청\n\n" +
            "분석 결과만 간단히 답변해주세요.",
            query
        );
        
        try {
            String response = llmService.getGeminiApiClient().generateContent(prompt)
                    .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                    .block();
            
            if (response != null && !response.trim().isEmpty()) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to analyze intent using LLM: {}", e.getMessage());
        }
        
        // 기본 분석 로직
        if (query.length() <= 10 && !query.contains(" ") && !query.contains("여행") && !query.contains("가고") && !query.contains("보고")) {
            return "KEYWORD";
        } else {
            return "SENTENCE";
        }
    }
    
    private String enhanceQuery(SearchRequestDto request, String intent) {
        if ("KEYWORD".equals(intent)) {
            // 키워드 기반 질의 강화
            String prompt = String.format(
                "다음 키워드를 바탕으로 여행 관련 질의를 강화해주세요:\n" +
                "키워드: %s\n\n" +
                "강화된 질의를 생성해주세요. 예시:\n" +
                "- '경복궁' → '경복궁 주변 관광지와 함께하는 서울 문화 여행'\n" +
                "- '부산' → '부산의 주요 관광지와 맛집을 둘러보는 여행'\n\n" +
                "강화된 질의만 답변해주세요.",
                request.getQuery()
            );
            
            try {
                String response = llmService.getGeminiApiClient().generateContent(prompt)
                        .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                        .block();
                
                if (response != null && !response.trim().isEmpty()) {
                    return response.trim();
                }
            } catch (Exception e) {
                log.warn("Failed to enhance keyword query using LLM: {}", e.getMessage());
            }
            
            return request.getQuery() + " 관광지 추천";
            
        } else {
            // 문장 기반 질의 강화
            String prompt = String.format(
                "다음 여행 관련 문장을 분석하여 구체적인 여행 계획 질의로 강화해주세요:\n" +
                "원본: %s\n\n" +
                "다음 정보를 포함하여 강화해주세요:\n" +
                "- 목적지\n" +
                "- 여행 일수\n" +
                "- 관심사\n" +
                "- 예산 범위\n" +
                "- 교통수단\n\n" +
                "강화된 질의만 답변해주세요.",
                request.getQuery()
            );
            
            try {
                String response = llmService.getGeminiApiClient().generateContent(prompt)
                        .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                        .block();
                
                if (response != null && !response.trim().isEmpty()) {
                    return response.trim();
                }
            } catch (Exception e) {
                log.warn("Failed to enhance sentence query using LLM: {}", e.getMessage());
            }
            
            return request.getQuery();
        }
    }
    
    private String searchTouristInfo(SearchRequestDto request, String enhancedQuery) {
        try {
            // 지역 코드 추출 (간단한 예시)
            String areaCd = extractAreaCode(request.getDestination());
            String signguCd = extractSignguCode(request.getDestination());
            
            if (areaCd != null && signguCd != null) {
                // 키워드 기반 검색
                String keywordContext = touristInfoService.buildTouristContextForRAG(
                        request.getQuery(), areaCd, signguCd).block();
                
                // 지역 기반 검색
                String areaContext = touristInfoService.buildAreaBasedContextForRAG(
                        areaCd, signguCd).block();
                
                return keywordContext + "\n\n" + areaContext;
            }
        } catch (Exception e) {
            log.warn("Failed to search tourist info: {}", e.getMessage());
        }
        
        return "관광지 정보를 찾을 수 없습니다.";
    }
    
    private Object generateResult(SearchRequestDto request, String enhancedQuery, String touristContext) {
        try {
            if ("KEYWORD".equals(analyzeIntent(request.getQuery()))) {
                // 키워드 기반 추천 결과 생성
                String prompt = String.format(
                    "다음 정보를 바탕으로 관광지 추천을 생성해주세요:\n\n" +
                    "키워드: %s\n" +
                    "강화된 질의: %s\n\n" +
                    "관광지 정보:\n%s\n\n" +
                    "추천 관광지 5개와 간단한 설명을 JSON 형식으로 제공해주세요.",
                    request.getQuery(), enhancedQuery, touristContext
                );
                
                String response = llmService.getGeminiApiClient().generateContent(prompt)
                        .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                        .block();
                
                return response != null ? response : "추천 결과를 생성할 수 없습니다.";
                
            } else {
                // 문장 기반 여행 계획 생성
                String prompt = String.format(
                    "다음 정보를 바탕으로 여행 계획을 생성해주세요:\n\n" +
                    "원본 요청: %s\n" +
                    "강화된 질의: %s\n\n" +
                    "관광지 정보:\n%s\n\n" +
                    "상세한 여행 계획을 JSON 형식으로 제공해주세요.",
                    request.getQuery(), enhancedQuery, touristContext
                );
                
                String response = llmService.getGeminiApiClient().generateContent(prompt)
                        .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                        .block();
                
                return response != null ? response : "여행 계획을 생성할 수 없습니다.";
            }
            
        } catch (Exception e) {
            log.error("Failed to generate result: {}", e.getMessage());
            return "결과 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
    
    private String extractAreaCode(String destination) {
        if (destination == null) return null;
        
        if (destination.contains("서울")) return "11";
        if (destination.contains("부산")) return "21";
        if (destination.contains("대구")) return "22";
        if (destination.contains("인천")) return "23";
        if (destination.contains("광주")) return "24";
        if (destination.contains("대전")) return "25";
        if (destination.contains("울산")) return "26";
        if (destination.contains("세종")) return "29";
        if (destination.contains("경기")) return "31";
        if (destination.contains("강원")) return "32";
        if (destination.contains("충북")) return "33";
        if (destination.contains("충남")) return "34";
        if (destination.contains("전북")) return "35";
        if (destination.contains("전남")) return "36";
        if (destination.contains("경북")) return "37";
        if (destination.contains("경남")) return "38";
        if (destination.contains("제주")) return "39";
        
        return null;
    }
    
    private String extractSignguCode(String destination) {
        if (destination == null) return null;
        
        // 간단한 예시 - 실제로는 더 정교한 매핑 필요
        if (destination.contains("강남")) return "11680";
        if (destination.contains("서초")) return "11650";
        if (destination.contains("종로")) return "11110";
        if (destination.contains("중구")) return "11140";
        if (destination.contains("용산")) return "11170";
        
        return null;
    }
    
    private SearchResponseDto createResponse(String status, String message, String step, Object data, List<String> progress) {
        return SearchResponseDto.builder()
                .status(status)
                .message(message)
                .step(step)
                .data(data)
                .timestamp(LocalDateTime.now())
                .progress(new ArrayList<>(progress))
                .build();
    }
} 