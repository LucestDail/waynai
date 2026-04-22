package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 외부 RAG 소스(관광공사/네이버) 수집 결과 요약.
 * SearchProgress UI 에서 라이브 리스트로 렌더링됩니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceSummaryDto {

    /** 소스 식별자: "tour" | "naver" */
    private String source;

    /** 총 수집 건수. */
    private int count;

    /** 상위 N건 미리보기 (제목 또는 관광지명). */
    private List<SourceItem> items;

    /** 관광공사: 지역/시군구 요약. 네이버: 검색 쿼리. */
    private String context;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceItem {
        private String title;
        private String subtitle;
        private String url;
    }
}
