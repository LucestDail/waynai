package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 의도 분석 결과와 네이버 검색 결과를 포함하는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAnalysisWithSearchDto {
    
    private IntentAnalysisDto intentAnalysis;
    private NaverBlogSearchDto naverSearchResult;
    private boolean hasNaverSearch;
}
