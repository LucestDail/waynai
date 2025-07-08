package com.waynai.demo.service;

import com.waynai.demo.dto.TourCourseRequestDto;
import com.waynai.demo.dto.TourCourseResponseDto;
import com.waynai.demo.dto.TouristSpotDto;
import com.waynai.demo.dto.SpotVisitDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourCourseService {
    
    private final TouristSpotService touristSpotService;
    private final LLMService llmService;
    
    public TourCourseResponseDto generateTourCourse(TourCourseRequestDto request) {
        log.info("Generating tour course for destination: {}, days: {}, theme: {}", 
                request.getDestination(), request.getDays(), request.getTheme());
        
        try {
            // 1. 관광지 정보 수집
            List<TouristSpotDto> availableSpots = touristSpotService.getTouristSpotsByDestination(
                    request.getDestination(), request.getTheme());
            
            if (availableSpots.isEmpty()) {
                throw new RuntimeException("No tourist spots found for the given destination and theme");
            }
            
            // 2. LLM을 통한 코스 생성
            String courseId = generateCourseId();
            TourCourseResponseDto course = llmService.generateCourse(request, availableSpots, courseId);
            
            // 3. 코스 최적화 및 검증
            course = optimizeCourse(course, request);
            
            log.info("Successfully generated tour course with ID: {}", courseId);
            return course;
            
        } catch (Exception e) {
            log.error("Failed to generate tour course: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate tour course", e);
        }
    }
    
    public TourCourseResponseDto getTourCourse(String courseId) {
        log.info("Retrieving tour course with ID: {}", courseId);
        
        // 실제 구현에서는 데이터베이스에서 조회
        // 현재는 예시 데이터 반환
        return createSampleCourse(courseId);
    }
    
    public List<TourCourseResponseDto> getTourCoursesByDestination(String destination) {
        log.info("Retrieving tour courses for destination: {}", destination);
        
        // 실제 구현에서는 데이터베이스에서 조회
        // 현재는 예시 데이터 반환
        return Arrays.asList(createSampleCourse("sample-course-1"));
    }
    
    public List<String> generateTravelTips(TourCourseRequestDto request) {
        log.info("Generating travel tips for destination: {}", request.getDestination());
        
        try {
            return llmService.generateTravelTips(request);
        } catch (Exception e) {
            log.error("Failed to generate travel tips: {}", e.getMessage(), e);
            // 기본 팁 반환
            List<String> defaultTips = new ArrayList<>();
            defaultTips.add("교통카드 준비");
            defaultTips.add("편안한 신발 착용");
            defaultTips.add("카메라 준비");
            return defaultTips;
        }
    }
    
    private String generateCourseId() {
        return "course-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    private TourCourseResponseDto optimizeCourse(TourCourseResponseDto course, TourCourseRequestDto request) {
        // 코스 최적화 로직 (거리, 시간, 예산 등 고려)
        log.debug("Optimizing course: {}", course.getCourseId());
        
        // 예시: 각 일정의 관광지 순서를 거리 기반으로 재정렬
        course.getDayPlans().forEach(dayPlan -> {
            if (dayPlan.getSpots() != null && dayPlan.getSpots().size() > 1) {
                dayPlan.setSpots(optimizeSpotOrder(dayPlan.getSpots()));
            }
        });
        
        return course;
    }
    
    private List<SpotVisitDto> optimizeSpotOrder(List<SpotVisitDto> spots) {
        // 간단한 거리 기반 최적화 (실제로는 TSP 알고리즘 등 사용)
        // 현재는 순서만 유지하고 향후 개선 예정
        return spots.stream()
                .sorted(Comparator.comparing(spot -> spot.getOrder()))
                .collect(Collectors.toList());
    }
    
    private TourCourseResponseDto createSampleCourse(String courseId) {
        return TourCourseResponseDto.builder()
                .courseId(courseId)
                .title("서울 문화 탐방 3일 코스")
                .destination("서울")
                .totalDays(3)
                .theme("문화")
                .summary("서울의 주요 문화 명소들을 3일간 탐방하는 코스입니다.")
                .createdAt(LocalDateTime.now())
                .estimatedBudget("30만원")
                .transportationInfo("지하철 및 버스 이용")
                .accommodationInfo("호텔 또는 게스트하우스")
                .tips(Arrays.asList("교통카드 준비", "편안한 신발 착용", "카메라 준비"))
                .weatherInfo("계절에 따라 적절한 옷차림 필요")
                .build();
    }
} 