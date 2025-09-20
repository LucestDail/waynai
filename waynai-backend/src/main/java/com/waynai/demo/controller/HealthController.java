package com.waynai.demo.controller;

import com.waynai.demo.client.TouristApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 헬스 체크 컨트롤러
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    private final TouristApiClient touristApiClient;
    
    @Value("${data.api.url.LocgoHubTarService1.areaBasedList1}")
    private String apiUrl;
    
    @Value("${data.api.url.LocgoHubTarService1.areaBasedList1.serviceKey}")
    private String serviceKey;
    
    public HealthController(TouristApiClient touristApiClient) {
        this.touristApiClient = touristApiClient;
    }
    
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("apiUrl", apiUrl);
        result.put("serviceKey", serviceKey != null ? serviceKey.substring(0, 10) + "..." : "null");
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/test-api")
    public ResponseEntity<Map<String, Object>> testApi() {
        Map<String, Object> result = new HashMap<>();
        try {
            var response = touristApiClient.getAreaBasedList("11", "11530", 1, 10);
            result.put("success", true);
            result.put("response", response);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }
        return ResponseEntity.ok(result);
    }
}
