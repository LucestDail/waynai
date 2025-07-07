package com.tripamigo.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripamigo.demo.client.GeminiApiClient;
import com.tripamigo.demo.dto.TourCourseRequestDto;
import com.tripamigo.demo.dto.TourCourseResponseDto;
import com.tripamigo.demo.dto.TouristSpotDto;
import com.tripamigo.demo.dto.DayPlanDto;
import com.tripamigo.demo.dto.SpotVisitDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {
    
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    
    public TourCourseResponseDto generateCourse(TourCourseRequestDto request, List<TouristSpotDto> availableSpots, String courseId) {
        log.info("Generating course using LLM for course ID: {}", courseId);
        
        try {
            // Gemini API를 사용하여 코스 생성
            String prompt = createCourseGenerationPrompt(request, availableSpots);
            String llmResponse = geminiApiClient.generateContent(prompt)
                    .map(geminiApiClient::extractTextFromResponse)
                    .block();
            
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                // LLM 응답을 파싱하여 코스 생성 (실패 시 샘플 데이터 사용)
                try {
                    return parseLLMResponse(llmResponse, request, courseId);
                } catch (Exception e) {
                    log.warn("Failed to parse LLM response, using sample data: {}", e.getMessage());
                }
            }
            
            // LLM 응답이 없거나 파싱 실패 시 샘플 데이터 사용
            return createSampleCourseResponse(request, availableSpots, courseId);
            
        } catch (Exception e) {
            log.error("Failed to generate course using LLM: {}", e.getMessage(), e);
            // LLM API 실패 시 샘플 데이터 사용
            return createSampleCourseResponse(request, availableSpots, courseId);
        }
    }
    
    private TourCourseResponseDto createSampleCourseResponse(TourCourseRequestDto request, List<TouristSpotDto> spots, String courseId) {
        List<DayPlanDto> dayPlans = new ArrayList<>();
        
        for (int day = 1; day <= request.getDays(); day++) {
            DayPlanDto dayPlan = createDayPlan(day, spots, request);
            dayPlans.add(dayPlan);
        }
        
        return TourCourseResponseDto.builder()
                .courseId(courseId)
                .title(request.getDestination() + " " + request.getTheme() + " 탐방 " + request.getDays() + "일 코스")
                .destination(request.getDestination())
                .totalDays(request.getDays())
                .theme(request.getTheme())
                .summary(request.getDestination() + "의 " + request.getTheme() + " 테마로 " + request.getDays() + "일간 즐길 수 있는 관광 코스입니다.")
                .dayPlans(dayPlans)
                .createdAt(LocalDateTime.now())
                .estimatedBudget("30만원")
                .transportationInfo(request.getTransportation() != null ? request.getTransportation() : "지하철 및 버스 이용")
                .accommodationInfo(request.getAccommodation() != null ? request.getAccommodation() : "호텔 또는 게스트하우스")
                .tips(Arrays.asList("교통카드 준비", "편안한 신발 착용", "카메라 준비"))
                .weatherInfo("계절에 따라 적절한 옷차림 필요")
                .build();
    }
    
    private DayPlanDto createDayPlan(int dayNumber, List<TouristSpotDto> spots, TourCourseRequestDto request) {
        List<SpotVisitDto> spotVisits = new ArrayList<>();
        
        // 각 날짜별로 2-3개 관광지 배정
        int spotsPerDay = Math.min(3, spots.size() / request.getDays() + 1);
        int startIndex = (dayNumber - 1) * spotsPerDay;
        
        for (int i = 0; i < spotsPerDay && startIndex + i < spots.size(); i++) {
            TouristSpotDto spot = spots.get(startIndex + i);
            SpotVisitDto spotVisit = createSpotVisit(spot, i + 1);
            spotVisits.add(spotVisit);
        }
        
        return DayPlanDto.builder()
                .dayNumber(dayNumber)
                .dayTitle(request.getDestination() + " " + dayNumber + "일차")
                .overview(request.getDestination() + "의 " + request.getTheme() + " 명소들을 탐방하는 " + dayNumber + "일차입니다.")
                .spots(spotVisits)
                .transportation("지하철 및 버스")
                .accommodation("호텔")
                .meals("아침: 호텔, 점심: 현지 맛집, 저녁: 현지 맛집")
                .estimatedCost("10만원")
                .tips("편안한 신발 착용, 충분한 휴식 취하기")
                .build();
    }
    
    private SpotVisitDto createSpotVisit(TouristSpotDto spot, int order) {
        return SpotVisitDto.builder()
                .spot(spot)
                .visitTime("09:00")
                .duration(spot.getEstimatedDuration() != null ? spot.getEstimatedDuration() : 60)
                .activity("관광 및 사진 촬영")
                .notes("입장료 확인 필요")
                .order(order)
                .build();
    }
    
    public String generateCourseDescription(TourCourseRequestDto request) {
        log.info("Generating course description for destination: {}", request.getDestination());
        
        try {
            String prompt = String.format(
                "다음 여행 정보에 맞는 매력적인 코스 설명을 한국어로 작성해주세요:\n" +
                "- 목적지: %s\n" +
                "- 테마: %s\n" +
                "- 여행 일수: %d일\n" +
                "- 교통수단: %s\n" +
                "- 여행 스타일: %s\n\n" +
                "설명은 2-3문장으로 간결하게 작성하고, 여행의 매력점을 강조해주세요.",
                request.getDestination(),
                request.getTheme(),
                request.getDays(),
                request.getTransportation() != null ? request.getTransportation() : "미정",
                request.getTravelStyle() != null ? request.getTravelStyle() : "일반"
            );
            
            String llmResponse = geminiApiClient.generateContent(prompt, 0.8, 300)
                    .map(geminiApiClient::extractTextFromResponse)
                    .block();
            
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                return llmResponse.trim();
            }
        } catch (Exception e) {
            log.warn("Failed to generate course description using LLM: {}", e.getMessage());
        }
        
        // 기본 설명 반환
        return request.getDestination() + "의 " + request.getTheme() + " 테마로 " + 
               request.getDays() + "일간 즐길 수 있는 관광 코스입니다. " +
               "다양한 명소들을 방문하며 현지의 문화와 자연을 체험할 수 있습니다.";
    }
    
    public List<String> generateTravelTips(TourCourseRequestDto request) {
        log.info("Generating travel tips for destination: {}", request.getDestination());
        
        try {
            String prompt = createTravelTipsPrompt(request);
            String llmResponse = geminiApiClient.generateContent(prompt, 0.8, 500)
                    .map(geminiApiClient::extractTextFromResponse)
                    .block();
            
            if (llmResponse != null && !llmResponse.trim().isEmpty()) {
                return parseTravelTips(llmResponse);
            }
        } catch (Exception e) {
            log.warn("Failed to generate travel tips using LLM: {}", e.getMessage());
        }
        
        // 기본 팁 반환
        List<String> tips = new ArrayList<>();
        tips.add("교통카드 준비");
        tips.add("편안한 신발 착용");
        tips.add("카메라 준비");
        
        if ("자연".equals(request.getTheme())) {
            tips.add("야외 활동용 의류 준비");
            tips.add("충분한 물 준비");
        } else if ("문화".equals(request.getTheme())) {
            tips.add("문화재 관람 예절 준수");
            tips.add("사전 예약 확인");
        }
        
        return tips;
    }
    
    private String createCourseGenerationPrompt(TourCourseRequestDto request, List<TouristSpotDto> availableSpots) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("당신은 전문 여행 코스 설계사입니다. 다음 정보를 바탕으로 상세한 관광 코스를 생성해주세요.\n\n");
        
        prompt.append("여행 정보:\n");
        prompt.append("- 목적지: ").append(request.getDestination()).append("\n");
        prompt.append("- 여행 일수: ").append(request.getDays()).append("일\n");
        prompt.append("- 테마: ").append(request.getTheme()).append("\n");
        if (request.getBudget() != null) prompt.append("- 예산: ").append(request.getBudget()).append("\n");
        if (request.getTransportation() != null) prompt.append("- 교통수단: ").append(request.getTransportation()).append("\n");
        if (request.getInterests() != null) prompt.append("- 관심사: ").append(String.join(", ", request.getInterests())).append("\n");
        if (request.getTravelStyle() != null) prompt.append("- 여행 스타일: ").append(request.getTravelStyle()).append("\n");
        
        prompt.append("\n사용 가능한 관광지:\n");
        for (TouristSpotDto spot : availableSpots) {
            prompt.append("- ").append(spot.getName()).append(": ").append(spot.getDescription())
                  .append(" (소요시간: ").append(spot.getEstimatedDuration()).append("분, 카테고리: ").append(spot.getCategory()).append(")\n");
        }
        
        prompt.append("\n요구사항:\n");
        prompt.append("1. 각 일차별로 2-3개 관광지를 배정하세요.\n");
        prompt.append("2. 관광지 간 이동 시간을 고려하세요.\n");
        prompt.append("3. 식사 시간과 휴식 시간을 포함하세요.\n");
        prompt.append("4. 예산과 교통수단을 고려하세요.\n");
        prompt.append("5. JSON 형식으로 응답하되, 다음 구조를 따르세요:\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"코스 제목\",\n");
        prompt.append("  \"summary\": \"코스 요약\",\n");
        prompt.append("  \"dayPlans\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"dayNumber\": 1,\n");
        prompt.append("      \"dayTitle\": \"1일차 제목\",\n");
        prompt.append("      \"overview\": \"1일차 개요\",\n");
        prompt.append("      \"spots\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"spotName\": \"관광지명\",\n");
        prompt.append("          \"visitTime\": \"방문시간\",\n");
        prompt.append("          \"duration\": 120,\n");
        prompt.append("          \"activity\": \"활동내용\",\n");
        prompt.append("          \"notes\": \"참고사항\"\n");
        prompt.append("        }\n");
        prompt.append("      ],\n");
        prompt.append("      \"transportation\": \"교통수단\",\n");
        prompt.append("      \"meals\": \"식사 정보\",\n");
        prompt.append("      \"estimatedCost\": \"예상 비용\",\n");
        prompt.append("      \"tips\": \"일차별 팁\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"estimatedBudget\": \"총 예상 비용\",\n");
        prompt.append("  \"transportationInfo\": \"교통 정보\",\n");
        prompt.append("  \"accommodationInfo\": \"숙박 정보\",\n");
        prompt.append("  \"tips\": [\"전체 팁1\", \"전체 팁2\"],\n");
        prompt.append("  \"weatherInfo\": \"날씨 정보\"\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }
    
    private String createTravelTipsPrompt(TourCourseRequestDto request) {
        return String.format(
            "다음 여행 정보에 맞는 실용적인 여행 팁 5개를 한국어로 제공해주세요:\n" +
            "- 목적지: %s\n" +
            "- 테마: %s\n" +
            "- 여행 일수: %d일\n" +
            "- 교통수단: %s\n" +
            "- 여행 스타일: %s\n\n" +
            "각 팁은 한 줄로 간결하게 작성하고, 번호를 매겨주세요.",
            request.getDestination(),
            request.getTheme(),
            request.getDays(),
            request.getTransportation() != null ? request.getTransportation() : "미정",
            request.getTravelStyle() != null ? request.getTravelStyle() : "일반"
        );
    }
    
    private TourCourseResponseDto parseLLMResponse(String llmResponse, TourCourseRequestDto request, String courseId) {
        try {
            // JSON 파싱 시도 (실제로는 더 복잡한 파싱 로직이 필요)
            log.info("Parsing LLM response for course generation");
            
            // 현재는 기본 구조만 반환하고, 실제 파싱은 향후 개선
            return createSampleCourseResponse(request, new ArrayList<>(), courseId);
            
        } catch (Exception e) {
            log.error("Failed to parse LLM response: {}", e.getMessage());
            throw new RuntimeException("LLM response parsing failed", e);
        }
    }
    
    private List<String> parseTravelTips(String llmResponse) {
        List<String> tips = new ArrayList<>();
        String[] lines = llmResponse.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^\\d+\\..*")) {
                // 번호가 있는 라인에서 팁 추출
                String tip = line.replaceFirst("^\\d+\\.\\s*", "");
                if (!tip.isEmpty()) {
                    tips.add(tip);
                }
            } else if (!line.isEmpty() && tips.size() < 5) {
                // 번호가 없는 라인도 팁으로 추가
                tips.add(line);
            }
        }
        
        return tips.isEmpty() ? Arrays.asList("교통카드 준비", "편안한 신발 착용", "카메라 준비") : tips;
    }
} 