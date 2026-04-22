package com.waynai.demo.controller;

import com.waynai.demo.dto.GpsNearbyRequestDto;
import com.waynai.demo.dto.GpsNearbyResponseDto;
import com.waynai.demo.service.GpsNearbyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 모바일 앱 GPS 기반 주변 추천 엔드포인트.
 * Flutter 앱이 현재 위치(lat/lng) 를 60~120초 간격 또는 300m 이동 시 전송한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/gps")
@RequiredArgsConstructor
public class GpsController {

    private final GpsNearbyService gpsNearbyService;

    /**
     * POST /api/gps/nearby — 기본 엔드포인트.
     * 요청 바디: { lat, lng, radiusMeters?, contentTypeId?, limit?, context? }
     */
    @PostMapping(value = "/nearby")
    public ResponseEntity<GpsNearbyResponseDto> nearby(@RequestBody GpsNearbyRequestDto request) {
        try {
            return ResponseEntity.ok(gpsNearbyService.findNearby(request));
        } catch (IllegalArgumentException e) {
            log.warn("[gps.nearby] 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[gps.nearby] 내부 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/gps/nearby — 수동 테스트 / 디버깅용 쿼리 파라미터 버전.
     */
    @GetMapping("/nearby")
    public ResponseEntity<GpsNearbyResponseDto> nearbyGet(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer radiusMeters,
            @RequestParam(required = false) String contentTypeId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String context) {
        GpsNearbyRequestDto req = new GpsNearbyRequestDto();
        req.setLat(lat);
        req.setLng(lng);
        req.setRadiusMeters(radiusMeters);
        req.setContentTypeId(contentTypeId);
        req.setLimit(limit);
        req.setContext(context);
        return nearby(req);
    }
}
