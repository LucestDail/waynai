package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 의도 분석 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAnalysisDto {
    
    private String intent; // "keyword", "area", "area_keyword"
    private String keyword; // 키워드 (해당하는 경우)
    private AreaInfo area; // 지역 정보 (해당하는 경우)
    private Double confidence; // 신뢰도 (0.0 ~ 1.0)
    private String reason; // 분석 이유 또는 오류 메시지
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AreaInfo {
        private String name; // 지역명
        private String code; // 지역코드
        private SigunguInfo sigungu; // 시군구 정보
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SigunguInfo {
            private String name; // 시군구명
            private String code; // 시군구코드
        }
    }
}
