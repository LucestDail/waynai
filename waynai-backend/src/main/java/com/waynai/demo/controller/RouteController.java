package com.waynai.demo.controller;

import com.waynai.demo.client.RoutingApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 실제 이동 경로(geometry) 조회 컨트롤러 — 프론트 지도에서 직선 대신 실제 도로 경로 렌더용.
 *
 * <p>예) GET /api/route?profile=foot-walking&points=34.669,135.502;34.66,135.50
 */
@Slf4j
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RoutingApiClient routingApiClient;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> route(
            @RequestParam String points,
            @RequestParam(required = false, defaultValue = "driving-car") String profile) {
        Map<String, Object> out = new HashMap<>();
        if (!routingApiClient.isEnabled()) {
            out.put("enabled", false);
            return out;
        }
        List<double[]> latLngs = new ArrayList<>();
        for (String p : points.split(";")) {
            String[] xy = p.split(",");
            if (xy.length == 2) {
                try {
                    latLngs.add(new double[]{Double.parseDouble(xy[0].trim()), Double.parseDouble(xy[1].trim())});
                } catch (NumberFormatException ignore) { }
            }
        }
        RoutingApiClient.RouteDetail d = routingApiClient.routeDetail(latLngs, profile);
        if (d == null) {
            out.put("enabled", true);
            out.put("geometry", List.of());
            return out;
        }
        out.put("enabled", true);
        out.put("geometry", d.geometry());     // [[lat,lng], ...]
        out.put("distanceMeters", d.distanceMeters());
        out.put("durationSeconds", d.durationSeconds());
        List<Map<String, Object>> legs = new ArrayList<>();
        for (RoutingApiClient.RouteSummary s : d.legs()) {
            Map<String, Object> m = new HashMap<>();
            m.put("distanceMeters", s.distanceMeters());
            m.put("durationSeconds", s.durationSeconds());
            legs.add(m);
        }
        out.put("legs", legs);
        return out;
    }
}
