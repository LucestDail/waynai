package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto {
    private String status; // "analyzing", "enhancing", "searching", "generating", "completed", "error"
    private String message;
    private String step; // "intent_analysis", "query_enhancement", "knowledge_search", "course_generation"
    private Object data;
    private LocalDateTime timestamp;
    private List<String> progress; // 진행 상황 로그
} 