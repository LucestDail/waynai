package com.waynai.demo.dto;

import lombok.Data;

/**
 * 모바일 GPS 주변 추천 요청.
 * 모바일 앱이 60~120초 주기 또는 사용자 이동 300m 이상 시 전송.
 */
@Data
public class GpsNearbyRequestDto {

    /** 위도 (latitude). 예: 37.5665 */
    private Double lat;

    /** 경도 (longitude). 예: 126.9780 */
    private Double lng;

    /** 반경(미터). 기본 1000m, 허용 20 ~ 20000. */
    private Integer radiusMeters;

    /** 콘텐츠 타입 (선택). 12 관광지, 14 문화시설, 28 레포츠, 32 숙박, 38 쇼핑, 39 음식점. */
    private String contentTypeId;

    /** 결과 최대 개수. 기본 15. */
    private Integer limit;

    /** 추가 컨텍스트(선택). 네이버 블로그 검색 시 결합 키워드. 예: "현지인 맛집" */
    private String context;
}
