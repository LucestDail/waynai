package com.waynai.demo.service;

import com.waynai.demo.dto.SearchRequestDto;
import com.waynai.demo.dto.SearchResponseDto;
import com.waynai.demo.util.AreaCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    
    private final LLMService llmService;
    private final TouristInfoService touristInfoService;
    private final AreaCodeUtil areaCodeUtil;
    private final ObjectMapper objectMapper;
    
    public SearchResponseDto processSearchSimple(SearchRequestDto request) {
        try {
            List<String> progress = new ArrayList<>();
            
            // 요청 정보 로깅
            log.info("=== SEARCH REQUEST START ===");
            log.info("Query: '{}'", request.getQuery());
            log.info("Destination: '{}'", request.getDestination());
            log.info("Theme: '{}'", request.getTheme());
            log.info("Days: {}", request.getDays());
            log.info("Budget: '{}'", request.getBudget());
            log.info("Transportation: '{}'", request.getTransportation());
            log.info("TravelStyle: '{}'", request.getTravelStyle());
            
            // 1단계: 지역 정보 추출 (Gemini 호출 #1)
            progress.add("사용자 입력 분석 및 지역 정보 추출 중...");
            log.info("=== STEP 1: AREA INFO EXTRACTION ===");
            List<Map<String, String>> areaInfoList = extractAreaInfoFromQuery(request.getQuery(), request.getDestination());
            log.info("Extracted area info: {}", areaInfoList);
            progress.add("지역 정보 추출 완료: " + areaInfoList.size() + "개 지역");
            
            // 2단계: 관광지 정보 수집 (API 호출)
            progress.add("관광지 정보 검색 중...");
            log.info("=== STEP 2: TOURIST INFO COLLECTION ===");
            String touristContext = searchTouristInfoForAreas(areaInfoList, request.getQuery());
            log.info("Tourist context length: {}", touristContext.length());
            progress.add("관광지 정보 검색 완료");
            
            // 3단계: 여행 계획 생성 (Gemini 호출 #2)
            progress.add("AI 기반 여행 계획 생성 중...");
            log.info("=== STEP 3: TRAVEL PLAN GENERATION ===");
            Object result = generateTravelPlan(request, touristContext, areaInfoList);
            log.info("Travel plan generated successfully");
            progress.add("AI 기반 여행 계획 생성 완료");
            
            log.info("=== SEARCH REQUEST COMPLETED ===");
            log.info("Total result length: {}", result != null ? result.toString().length() : 0);
            
            return createResponse("completed", "검색 완료", "course_generation", result, progress);
            
        } catch (Exception e) {
            log.error("=== SEARCH REQUEST FAILED ===");
            log.error("Error: {}", e.getMessage(), e);
            return createResponse("error", "검색 처리 중 오류가 발생했습니다: " + e.getMessage(), "error", null, new ArrayList<>());
        }
    }
    
    /**
     * 1단계: Gemini에게 지역 정보 추출 요청 (Gemini 호출 #1)
     */
    private List<Map<String, String>> extractAreaInfoFromQuery(String query, String destination) {
        try {
            // query와 destination을 결합하여 지역 정보 추출
            String combinedInput = query;
            if (destination != null && !destination.trim().isEmpty()) {
                combinedInput = query + " " + destination;
            }
            
            log.info("Extracting area info from combined input: '{}'", combinedInput);
            
            String prompt = String.format(
                "다음 사용자 입력에서 지역 정보를 추출하여 JSON 배열 형태로 반환해주세요.\n\n" +
                "사용자 입력: %s\n\n" +
                "다음 형식으로 JSON 배열을 반환해주세요:\n" +
                "[\n" +
                "  {\n" +
                "    \"areaName\": \"지역명 (예: 부산, 서울, 제주도)\",\n" +
                "    \"sigunguName\": \"시군구명 (예: 해운대구, 강남구, 제주시)\"\n" +
                "  }\n" +
                "]\n\n" +
                "지침:\n" +
                "1. 사용자 입력에서 언급된 지역을 추출하세요\n" +
                "2. 지역명과 시군구명을 분리하여 추출하세요\n" +
                "3. 시군구명이 명확하지 않으면 해당 지역의 대표적인 시군구명을 추론하여 설정하세요\n" +
                "4. 여러 지역이 언급되면 각각을 배열에 포함하세요\n" +
                "5. 지역 정보가 없으면 기본값으로 [{\"areaName\": \"부산\", \"sigunguName\": \"해운대구\"}]를 반환하세요\n" +
                "6. 반드시 유효한 JSON 배열 형태로만 응답하세요\n" +
                "7. 코드 블록 표시(```) 없이 순수 JSON만 반환하세요\n" +
                "8. areaName과 sigunguName은 반드시 채워서 반환해야 합니다\n\n" +
                "추가 지침:\n" +
                "- 경복궁, 창덕궁, 창경궁, 덕수궁, 경희궁 등 궁궐 → 해당 지역 추론 및 반환, sigunguName: \"종로구\"\n" +
                "- 해운대, 광안리, 송도, 다대포 등 해수욕장 → 해당 지역 추론 및 반환, sigunguName: 해당 구명\n" +
                "- 명동, 강남, 홍대, 이태원 등 상권 → 해당 상권과 연관된 지역 추론 및 반환, sigunguName: 해당 구명\n" +
                "- 제주도, 제주시, 서귀포시 → 제주특별자치도, sigunguName: 제주시 또는 서귀포시\n" +
                "- 부산역, 서울역, 대구역 등 역명 → 해당 도시, sigunguName: 해당 구명 추론\n" +
                "- 특정 지역명이 언급되지 않으면 일반적인 관광지로 판단하여 기본 학습된 지식을 판단으로 지역명을 추론 및 반환해야 합니다\n" +
                "- 모든 응답에서 sigunguName은 빈 문자열이 아닌 구체적인 시군구명을 반환해야 합니다\n\n" +
                "예시:\n" +
                "- \"경복궁\" → [{\"areaName\": \"서울\", \"sigunguName\": \"종로구\"}]\n" +
                "- \"해운대\" → [{\"areaName\": \"부산\", \"sigunguName\": \"해운대구\"}]\n" +
                "- \"강남\" → [{\"areaName\": \"서울\", \"sigunguName\": \"강남구\"}]\n" +
                "- \"제주도\" → [{\"areaName\": \"제주특별자치도\", \"sigunguName\": \"제주시\"}]\n" +
                "- \"부산\" → [{\"areaName\": \"부산\", \"sigunguName\": \"해운대구\"}]\n" +
                "- \"서울\" → [{\"areaName\": \"서울\", \"sigunguName\": \"종로구\"}]",
                combinedInput != null ? combinedInput : "관광지 추천"
            );
            
            log.info("Calling Gemini API for area info extraction...");
            String response = llmService.getGeminiApiClient().generateContent(prompt)
                    .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                    .block();
            
            if (response != null) {
                log.info("Gemini API call successful. Raw response length: {}", response.length());
                
                // ```json 코드 블록 제거
                String cleanResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                log.info("Cleaned response length: {}", cleanResponse.length());
                
                // JSON 파싱
                List<Map<String, String>> areaInfoList = objectMapper.readValue(
                    cleanResponse, new TypeReference<List<Map<String, String>>>() {}
                );
                log.info("Successfully parsed area info: {}", areaInfoList);
                return areaInfoList;
            } else {
                log.warn("Gemini API returned null response for area info extraction");
            }
            
        } catch (Exception e) {
            log.error("Failed to extract area info from Gemini: {}", e.getMessage(), e);
        }
        
        // 기본값 반환
        log.info("Using default area info: 부산 해운대구");
        List<Map<String, String>> defaultAreaInfo = new ArrayList<>();
        defaultAreaInfo.add(Map.of("areaName", "부산", "sigunguName", "해운대구"));
        return defaultAreaInfo;
    }
    
    /**
     * 2단계: 추출된 지역 정보로 관광지 API 호출
     */
    private String searchTouristInfoForAreas(List<Map<String, String>> areaInfoList, String originalQuery) {
        StringBuilder contextBuilder = new StringBuilder();
        
        log.info("Processing {} areas for tourist info collection", areaInfoList.size());
        
        for (int i = 0; i < areaInfoList.size(); i++) {
            Map<String, String> areaInfo = areaInfoList.get(i);
            String areaName = areaInfo.get("areaName");
            String sigunguName = areaInfo.get("sigunguName");
            
            log.info("--- Processing area {}/{}: {} {} ---", i + 1, areaInfoList.size(), areaName, sigunguName);
            
            try {
                // 지역 코드 매칭
                log.info("Looking up area code for: {} - {}", areaName, sigunguName);
                AreaCodeUtil.AreaCodeInfo areaCodeInfo = areaCodeUtil.findAreaCodeByName(areaName, sigunguName);
                
                if (areaCodeInfo != null) {
                    String areaCd = areaCodeInfo.getAreaCode();
                    String signguCd = areaCodeInfo.getSignguCode();
                    
                    log.info("Found area code: {} - {} ({} - {})", 
                            areaName, sigunguName, areaCd, signguCd);
                    
                    // 키워드 기반 검색
                    log.info("Calling keyword-based tourist info API...");
                    String keywordContext = touristInfoService.buildTouristContextForRAG(
                            originalQuery, areaCd, signguCd).block();
                    log.info("Keyword-based search completed. Context length: {}", keywordContext.length());
                    contextBuilder.append("=== ").append(areaName).append(" ").append(sigunguName).append(" 관광지 정보 ===\n");
                    contextBuilder.append(keywordContext).append("\n\n");
                    
                    // 지역 기반 검색
                    log.info("Calling area-based tourist info API...");
                    String areaContext = touristInfoService.buildAreaBasedContextForRAG(
                            areaCd, signguCd).block();
                    log.info("Area-based search completed. Context length: {}", areaContext.length());
                    contextBuilder.append("=== ").append(areaName).append(" ").append(sigunguName).append(" 지역 기반 관광지 ===\n");
                    contextBuilder.append(areaContext).append("\n\n");
                    
                } else {
                    log.warn("Area code not found for: {} - {}", areaName, sigunguName);
                    contextBuilder.append("=== ").append(areaName).append(" 관광지 정보 ===\n");
                    contextBuilder.append("해당 지역의 관광지 정보를 찾을 수 없습니다.\n\n");
                }
                
            } catch (Exception e) {
                log.error("Failed to search tourist info for area {} - {}: {}", 
                        areaName, sigunguName, e.getMessage(), e);
                contextBuilder.append("=== ").append(areaName).append(" 관광지 정보 ===\n");
                contextBuilder.append("관광지 정보 검색 중 오류가 발생했습니다.\n\n");
            }
        }
        
        String result = contextBuilder.toString();
        log.info("Tourist context collection completed. Total length: {}", result.length());
        return result;
    }
    
    /**
     * 3단계: 받아온 정보로 여행 계획 생성 (Gemini 호출 #2)
     */
    private Object generateTravelPlan(SearchRequestDto request, String touristContext, List<Map<String, String>> areaInfoList) {
        try {
            // 지역 정보 요약
            StringBuilder areaSummary = new StringBuilder();
            for (Map<String, String> areaInfo : areaInfoList) {
                areaSummary.append(areaInfo.get("areaName"));
                if (areaInfo.get("sigunguName") != null && !areaInfo.get("sigunguName").isEmpty()) {
                    areaSummary.append(" ").append(areaInfo.get("sigunguName"));
                }
                areaSummary.append(", ");
            }
            String targetAreas = areaSummary.toString().replaceAll(", $", "");
            
            // 여행 일수 설정
            int travelDays = request.getDays() != null ? request.getDays() : 1;
            String durationText;
            switch (travelDays) {
                case 1:
                    durationText = "당일치기";
                    break;
                case 2:
                    durationText = "1박2일";
                    break;
                case 3:
                    durationText = "2박3일";
                    break;
                case 4:
                    durationText = "3박4일";
                    break;
                default:
                    durationText = travelDays + "박 " + (travelDays + 1) + "일";
            }
            
            log.info("Generating travel plan for: {} days to {}", travelDays, targetAreas);
            log.info("Tourist context length: {}", touristContext.length());
            
            String prompt = String.format(
                "다음 정보를 바탕으로 상세한 여행 계획을 생성해주세요:\n\n" +
                "원본 요청: %s\n" +
                "목적 지역: %s\n" +
                "여행 일수: %d일\n\n" +
                "관광지 정보:\n%s\n\n" +
                "위 정보를 바탕으로 %d일간의 상세한 여행 계획을 JSON 형식으로 제공해주세요. " +
                "응답은 다음 형식으로 해주세요:\n" +
                "{\n" +
                "  \"type\": \"travel_plan\",\n" +
                "  \"destination\": \"목적지\",\n" +
                "  \"duration\": \"%s\",\n" +
                "  \"summary\": \"여행 요약\",\n" +
                "  \"budget\": \"예상 비용\",\n" +
                "  \"itinerary\": [\n" +
                "    {\n" +
                "      \"day\": 1,\n" +
                "      \"title\": \"1일차\",\n" +
                "      \"overview\": \"일차별 개요\",\n" +
                "      \"activities\": [\"활동1\", \"활동2\"],\n" +
                "      \"spots\": [\"방문지1\", \"방문지2\"]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"tips\": [\"팁1\", \"팁2\"],\n" +
                "  \"transportation\": \"교통 정보\",\n" +
                "  \"accommodation\": \"숙박 정보\"\n" +
                "}\n\n" +
                "지침:\n" +
                "1. 정확히 %d일간의 여행 계획을 생성하세요\n" +
                "2. 각 일차별로 적절한 관광지와 활동을 배치하세요\n" +
                "3. 여행 일수가 1일인 경우 당일 여행으로, 2일 이상인 경우 숙박 계획을 포함하세요\n" +
                "4. 예산과 교통수단을 현실적으로 설정하세요",
                request.getQuery(), targetAreas, travelDays, touristContext, travelDays, durationText, travelDays
            );
            
            log.info("Calling Gemini API for travel plan generation...");
            String response = llmService.getGeminiApiClient().generateContent(prompt)
                    .map(llmService.getGeminiApiClient()::extractTextFromResponse)
                    .block();
            
            if (response != null) {
                log.info("Gemini API call successful. Response length: {}", response.length());
                return response;
            } else {
                log.warn("Gemini API returned null response, using default plan");
                return createDefaultTravelPlan(request.getQuery(), targetAreas, request.getDays());
            }
            
        } catch (Exception e) {
            log.error("Failed to generate travel plan: {}", e.getMessage(), e);
            log.info("Using default travel plan as fallback");
            return createDefaultTravelPlan(request.getQuery(), "부산 해운대구", request.getDays());
        }
    }
    
    private String createDefaultTravelPlan(String query, String destination, Integer days) {
        int travelDays = days != null ? days : 1;
        String durationText = travelDays == 1 ? "당일 여행" : travelDays + "박 " + (travelDays + 1) + "일";
        
        StringBuilder itineraryBuilder = new StringBuilder();
        for (int day = 1; day <= travelDays; day++) {
            itineraryBuilder.append("    {\n");
            itineraryBuilder.append("      \"day\": ").append(day).append(",\n");
            itineraryBuilder.append("      \"title\": \"").append(day).append("일차\",\n");
            
            if (day == 1) {
                itineraryBuilder.append("      \"overview\": \"도착 및 주요 관광지 방문\",\n");
                itineraryBuilder.append("      \"activities\": [\"도착\", \"주요 관광지 방문\", \"저녁 식사\"],\n");
                itineraryBuilder.append("      \"spots\": [\"역\", \"주요 관광지\", \"맛집\"]\n");
            } else if (day == travelDays) {
                itineraryBuilder.append("      \"overview\": \"마지막 관광지 방문 및 출발\",\n");
                itineraryBuilder.append("      \"activities\": [\"아침 식사\", \"마지막 관광지 방문\", \"출발\"],\n");
                itineraryBuilder.append("      \"spots\": [\"호텔\", \"마지막 관광지\", \"역\"]\n");
            } else {
                itineraryBuilder.append("      \"overview\": \"추가 관광지 방문\",\n");
                itineraryBuilder.append("      \"activities\": [\"아침 식사\", \"추가 관광지 방문\", \"저녁 식사\"],\n");
                itineraryBuilder.append("      \"spots\": [\"호텔\", \"추가 관광지\", \"맛집\"]\n");
            }
            
            itineraryBuilder.append("    }");
            if (day < travelDays) {
                itineraryBuilder.append(",");
            }
            itineraryBuilder.append("\n");
        }
        
        return String.format(
            "{\n" +
            "  \"type\": \"travel_plan\",\n" +
            "  \"destination\": \"%s\",\n" +
            "  \"duration\": \"%s\",\n" +
            "  \"summary\": \"%s에 대한 %d일간의 기본 여행 계획입니다.\",\n" +
            "  \"budget\": \"%d만원\",\n" +
            "  \"itinerary\": [\n%s  ],\n" +
            "  \"tips\": [\"교통카드 준비\", \"편안한 신발 착용\", \"카메라 준비\"],\n" +
            "  \"transportation\": \"지하철 및 버스\",\n" +
            "  \"accommodation\": \"%s\"\n" +
            "}",
            destination, durationText, query, travelDays, travelDays * 15, itineraryBuilder.toString(),
            travelDays > 1 ? "호텔" : "당일 여행"
        );
    }
    
    private SearchResponseDto createResponse(String status, String message, String step, Object data, List<String> progress) {
        return SearchResponseDto.builder()
                .status(status)
                .message(message)
                .step(step)
                .data(data)
                .timestamp(LocalDateTime.now())
                .progress(progress)
                .build();
    }
} 