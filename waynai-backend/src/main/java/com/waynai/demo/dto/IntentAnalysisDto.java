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
    private AreaInfo area; // 지역 정보 (국내인 경우)
    private Boolean international; // 해외 목적지 여부 (true 면 한국관광공사 RAG 스킵)
    private String destination; // 도착지명 (국내/해외 공통, 예: "부산", "오사카", "파리")
    // --- 자연어에서 추출한 여행 조건 (에이전틱 처리, 없으면 null) ---
    private Integer days;        // 총 여행 일수 (예: "2박3일" → 3). 미지정 시 null.
    private String origin;       // 출발지명 (예: "서울", "인천"). 미지정 시 null → 기본 서울.
    private String departDate;   // 출발일 (YYYY-MM 또는 YYYY-MM-DD). 미지정 시 null.
    private String returnDate;   // 귀국일 (YYYY-MM 또는 YYYY-MM-DD). 미지정 시 null.
    private String style;        // 여행 스타일 (예: "배낭여행","신혼여행","효도여행","도보","가족").
    private String budgetLevel;  // 예산 수준 ("저렴"|"보통"|"고급") 또는 자유 서술.
    private String companions;   // 동반 유형 (예: "커플","가족","친구","혼자").
    private java.util.List<Segment> segments; // 다권역/장기 여행 시 권역별 분해 (없으면 단일 생성)
    private Double confidence; // 신뢰도 (0.0 ~ 1.0)
    private String reason; // 분석 이유 또는 오류 메시지

    /** 권역(구간) — 다도시/장기 여행을 나눠 상세 생성하기 위한 단위. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private String title;                 // 권역명 (예: "로마·바티칸")
        private Integer days;                 // 이 권역 일수
        private String dates;                 // 기간 (예: "3/30~4/2", 있으면)
        private String area;                  // 대표 지역/도시
        private String keywords;              // 이 권역 키워드
        private java.util.List<String> mustInclude; // 사용자가 명시한 필수 포함 장소·식당명
    }
    
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
