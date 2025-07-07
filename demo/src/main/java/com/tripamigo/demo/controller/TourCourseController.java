package com.tripamigo.demo.controller;

import com.tripamigo.demo.dto.ApiResponseDto;
import com.tripamigo.demo.dto.TourCourseRequestDto;
import com.tripamigo.demo.dto.TourCourseResponseDto;
import com.tripamigo.demo.service.TourCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tour-courses")
@RequiredArgsConstructor
public class TourCourseController {
    
    private final TourCourseService tourCourseService;
    
    @PostMapping
    public ResponseEntity<ApiResponseDto<TourCourseResponseDto>> generateTourCourse(
            @Valid @RequestBody TourCourseRequestDto request) {
        
        log.info("Received tour course generation request for destination: {}", request.getDestination());
        
        try {
            TourCourseResponseDto course = tourCourseService.generateTourCourse(request);
            return ResponseEntity.ok(ApiResponseDto.success(course, "관광 코스가 성공적으로 생성되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to generate tour course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광 코스 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponseDto<TourCourseResponseDto>> getTourCourse(@PathVariable String courseId) {
        log.info("Retrieving tour course with ID: {}", courseId);
        
        try {
            TourCourseResponseDto course = tourCourseService.getTourCourse(courseId);
            
            if (course == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDto.error("관광 코스를 찾을 수 없습니다.", "COURSE_NOT_FOUND"));
            }
            
            return ResponseEntity.ok(ApiResponseDto.success(course, "관광 코스를 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to retrieve tour course: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광 코스 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/destination/{destination}")
    public ResponseEntity<ApiResponseDto<List<TourCourseResponseDto>>> getTourCoursesByDestination(
            @PathVariable String destination) {
        
        log.info("Retrieving tour courses for destination: {}", destination);
        
        try {
            List<TourCourseResponseDto> courses = tourCourseService.getTourCoursesByDestination(destination);
            return ResponseEntity.ok(ApiResponseDto.success(courses, "관광 코스 목록을 성공적으로 조회했습니다."));
            
        } catch (Exception e) {
            log.error("Failed to retrieve tour courses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("관광 코스 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/travel-tips")
    public ResponseEntity<ApiResponseDto<List<String>>> generateTravelTips(
            @Valid @RequestBody TourCourseRequestDto request) {
        
        log.info("Received travel tips generation request for destination: {}", request.getDestination());
        
        try {
            List<String> tips = tourCourseService.generateTravelTips(request);
            return ResponseEntity.ok(ApiResponseDto.success(tips, "여행 팁이 성공적으로 생성되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to generate travel tips: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDto.error("여행 팁 생성에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponseDto.success("Tour Course Service is running", "서비스가 정상적으로 동작 중입니다."));
    }
} 