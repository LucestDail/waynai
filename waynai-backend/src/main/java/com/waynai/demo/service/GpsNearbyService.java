package com.waynai.demo.service;

import com.waynai.demo.client.LocationBasedApiClient;
import com.waynai.demo.client.NaverApiClient;
import com.waynai.demo.dto.GpsNearbyRequestDto;
import com.waynai.demo.dto.GpsNearbyResponseDto;
import com.waynai.demo.dto.LocationBasedResponseDto;
import com.waynai.demo.dto.LocationBasedSpotDto;
import com.waynai.demo.dto.NaverBlogSearchDto;
import com.waynai.demo.dto.NaverLocalSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 모바일 GPS 기반 주변 추천 파이프라인.
 * 좌표기반 관광공사 POI + 네이버 블로그(좌표 역지오코딩 불가하므로 클라이언트 힌트/타이틀 결합) 병렬 조회.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GpsNearbyService {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final int DEFAULT_RADIUS_M = 1000;
    private static final int MIN_RADIUS_M = 20;
    private static final int MAX_RADIUS_M = 20000;
    private static final int DEFAULT_LIMIT = 15;
    private static final int MAX_LIMIT = 50;

    // 한국관광공사 contentTypeId 라벨
    private static final Map<String, String> CONTENT_TYPE_LABEL = Map.of(
            "12", "관광지",
            "14", "문화시설",
            "15", "축제/공연",
            "25", "여행코스",
            "28", "레포츠",
            "32", "숙박",
            "38", "쇼핑",
            "39", "음식점"
    );

    private final LocationBasedApiClient locationClient;
    private final NaverApiClient naverApiClient;

    public GpsNearbyResponseDto findNearby(GpsNearbyRequestDto req) {
        validate(req);

        int radius = clampRadius(req.getRadiusMeters());
        int limit = clampLimit(req.getLimit());
        String contentTypeId = blankToNull(req.getContentTypeId());
        double lat = req.getLat();
        double lng = req.getLng();
        String context = blankToNull(req.getContext());

        log.info("[gps.nearby] lat={}, lng={}, radius={}m, contentTypeId={}, limit={}, context={}",
                lat, lng, radius, contentTypeId, limit, context);

        // 관광공사 locationBasedList2 는 mapX=경도, mapY=위도. 실패 시 Naver Local 폴백.
        final String effectiveContext = context == null
                ? "여행지 관광지 맛집"
                : context;
        CompletableFuture<List<GpsNearbyResponseDto.NearbySpot>> spotsFut =
                CompletableFuture.supplyAsync(() -> {
                    List<GpsNearbyResponseDto.NearbySpot> spots =
                            safeCollectSpots(lng, lat, radius, contentTypeId, limit);
                    if (!spots.isEmpty()) return spots;
                    log.info("[gps.nearby] 관광공사 0건 → Naver Local 폴백");
                    return safeCollectLocalSpots(lat, lng, radius, effectiveContext, limit);
                });

        CompletableFuture<List<GpsNearbyResponseDto.NearbyBlog>> blogsFut =
                CompletableFuture.supplyAsync(() ->
                        safeCollectBlogs(effectiveContext, limit));

        CompletableFuture.allOf(spotsFut, blogsFut).join();

        List<GpsNearbyResponseDto.NearbySpot> spots = spotsFut.join();
        List<GpsNearbyResponseDto.NearbyBlog> blogs = blogsFut.join();

        return GpsNearbyResponseDto.builder()
                .lat(lat)
                .lng(lng)
                .radiusMeters(radius)
                .spotCount(spots.size())
                .blogCount(blogs.size())
                .spots(spots)
                .blogs(blogs)
                .build();
    }

    private void validate(GpsNearbyRequestDto req) {
        if (req == null || req.getLat() == null || req.getLng() == null) {
            throw new IllegalArgumentException("lat, lng 는 필수입니다.");
        }
        double lat = req.getLat();
        double lng = req.getLng();
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("lat 범위 오류(-90~90): " + lat);
        }
        if (lng < -180.0 || lng > 180.0) {
            throw new IllegalArgumentException("lng 범위 오류(-180~180): " + lng);
        }
    }

    private int clampRadius(Integer r) {
        if (r == null) return DEFAULT_RADIUS_M;
        return Math.max(MIN_RADIUS_M, Math.min(MAX_RADIUS_M, r));
    }

    private int clampLimit(Integer l) {
        if (l == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(MAX_LIMIT, l));
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private List<GpsNearbyResponseDto.NearbySpot> safeCollectSpots(double mapX, double mapY,
                                                                   int radius, String contentTypeId,
                                                                   int limit) {
        try {
            LocationBasedResponseDto resp = locationClient.getLocationBasedList(
                    mapX, mapY, radius, contentTypeId, 1, limit);

            if (resp == null || resp.getResponse() == null
                    || resp.getResponse().getBody() == null
                    || resp.getResponse().getBody().getItems() == null
                    || resp.getResponse().getBody().getItems().getItem() == null) {
                return Collections.emptyList();
            }

            List<LocationBasedSpotDto> items = resp.getResponse().getBody().getItems().getItem();
            List<GpsNearbyResponseDto.NearbySpot> out = new ArrayList<>(items.size());
            for (LocationBasedSpotDto it : items) {
                out.add(mapSpot(it));
            }
            return out;
        } catch (Exception e) {
            log.warn("[gps.nearby] 관광공사 좌표기반 조회 실패 (빈 리스트 반환): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private GpsNearbyResponseDto.NearbySpot mapSpot(LocationBasedSpotDto it) {
        Double lat = parseDouble(it.getMapY());
        Double lng = parseDouble(it.getMapX());
        Integer dist = parseInt(it.getDist());

        String fullAddr = joinNonBlank(it.getAddr1(), it.getAddr2());
        String label = CONTENT_TYPE_LABEL.getOrDefault(
                String.valueOf(it.getContentTypeId()), "POI");

        return GpsNearbyResponseDto.NearbySpot.builder()
                .id(it.getContentId())
                .title(it.getTitle())
                .address(fullAddr)
                .category(joinNonBlank(it.getCat2(), it.getCat3()))
                .contentTypeLabel(label)
                .lat(lat)
                .lng(lng)
                .distanceMeters(dist)
                .thumbnail(firstNonBlank(it.getFirstImage(), it.getFirstImage2()))
                .tel(it.getTel())
                .build();
    }

    /**
     * 네이버 Local API 폴백 — 관광공사 좌표기반 API 권한이 없을 때 사용.
     * query 에 사용자 context 를 담아 호출하고, 반환된 mapx/mapy(1e7*경위도) 에
     * 하버사인 공식으로 사용자 좌표 대비 거리를 계산하여 radius 내 항목만 반환.
     */
    private List<GpsNearbyResponseDto.NearbySpot> safeCollectLocalSpots(double userLat, double userLng,
                                                                       int radius, String query, int limit) {
        try {
            NaverLocalSearchDto resp = naverApiClient.searchLocal(query, Math.min(limit, 5), "random");
            if (resp == null || resp.getItems() == null || resp.getItems().isEmpty()) {
                return Collections.emptyList();
            }
            List<GpsNearbyResponseDto.NearbySpot> out = new ArrayList<>(resp.getItems().size());
            for (NaverLocalSearchDto.LocalItem it : resp.getItems()) {
                Double lng = divideBy1e7(it.getMapx());
                Double lat = divideBy1e7(it.getMapy());
                Integer dist = null;
                if (lat != null && lng != null) {
                    dist = (int) Math.round(haversineMeters(userLat, userLng, lat, lng));
                    if (dist > radius) continue; // 반경 밖은 제외
                }
                out.add(GpsNearbyResponseDto.NearbySpot.builder()
                        .id(null)
                        .title(stripHtml(it.getTitle()))
                        .address(firstNonBlank(it.getRoadAddress(), it.getAddress()))
                        .category(it.getCategory())
                        .contentTypeLabel(naverCategoryToLabel(it.getCategory()))
                        .lat(lat)
                        .lng(lng)
                        .distanceMeters(dist)
                        .thumbnail(null)
                        .tel(it.getTelephone())
                        .build());
            }
            // 거리순 정렬
            out.sort((a, b) -> {
                Integer da = a.getDistanceMeters() == null ? Integer.MAX_VALUE : a.getDistanceMeters();
                Integer db = b.getDistanceMeters() == null ? Integer.MAX_VALUE : b.getDistanceMeters();
                return Integer.compare(da, db);
            });
            return out;
        } catch (Exception e) {
            log.warn("[gps.nearby] Naver Local 폴백 실패 (빈 리스트 반환): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static Double divideBy1e7(String s) {
        Double v = parseDouble(s);
        if (v == null) return null;
        return v / 1.0e7;
    }

    /** 하버사인 공식 — 두 좌표 사이 거리(미터) 계산. */
    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double R = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static String naverCategoryToLabel(String cat) {
        if (cat == null) return "장소";
        if (cat.contains("음식") || cat.contains("카페")) return "음식점";
        if (cat.contains("숙박") || cat.contains("호텔")) return "숙박";
        if (cat.contains("쇼핑") || cat.contains("백화점")) return "쇼핑";
        if (cat.contains("문화") || cat.contains("공연")) return "문화시설";
        if (cat.contains("관광") || cat.contains("여행")) return "관광지";
        return "장소";
    }

    private List<GpsNearbyResponseDto.NearbyBlog> safeCollectBlogs(String context, int limit) {
        if (context == null) {
            return Collections.emptyList();
        }
        try {
            NaverBlogSearchDto resp = naverApiClient.searchBlog(context, Math.min(limit, 10), 1, "sim");
            if (resp == null || resp.getItems() == null) return Collections.emptyList();
            List<GpsNearbyResponseDto.NearbyBlog> out = new ArrayList<>(resp.getItems().size());
            for (NaverBlogSearchDto.BlogItem b : resp.getItems()) {
                out.add(GpsNearbyResponseDto.NearbyBlog.builder()
                        .title(stripHtml(b.getTitle()))
                        .url(b.getLink())
                        .description(stripHtml(b.getDescription()))
                        .bloggerName(b.getBloggerName())
                        .postdate(b.getPostDate())
                        .build());
            }
            return out;
        } catch (Exception e) {
            log.warn("[gps.nearby] 네이버 블로그 조회 실패 (빈 리스트 반환): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private static String joinNonBlank(String a, String b) {
        if (a == null || a.isBlank()) return b;
        if (b == null || b.isBlank()) return a;
        return a + " " + b;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static String stripHtml(String s) {
        if (s == null) return null;
        return HTML_TAG.matcher(s).replaceAll("");
    }
}
