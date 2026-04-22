package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * /api/gps/nearby 응답 DTO.
 * 모바일 앱이 화면에 바로 바인딩할 수 있도록 정규화된 형태로 반환.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsNearbyResponseDto {

    private double lat;
    private double lng;
    private int radiusMeters;
    private int spotCount;
    private int blogCount;

    /** 주변 관광지/음식/숙박 POI 리스트 (거리순) */
    private List<NearbySpot> spots;

    /** 위치 기반 네이버 블로그 포스트 */
    private List<NearbyBlog> blogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearbySpot {
        private String id;
        private String title;
        private String address;
        private String category;
        private String contentTypeLabel;
        private Double lat;
        private Double lng;
        /** 사용자로부터의 거리(미터). null 이면 미계산. */
        private Integer distanceMeters;
        private String thumbnail;
        private String tel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearbyBlog {
        private String title;
        private String url;
        private String description;
        private String bloggerName;
        private String postdate;
    }
}
