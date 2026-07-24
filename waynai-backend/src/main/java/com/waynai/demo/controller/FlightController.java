package com.waynai.demo.controller;

import com.waynai.demo.dto.FlightOfferDto;
import com.waynai.demo.service.FlightSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 항공권 검색 컨트롤러 (테스트 및 프론트 FlightInfo 카드용).
 *
 * <p>예) GET /api/flights?origin=서울&destination=오사카&departDate=2026-08&returnDate=2026-08
 */
@Slf4j
@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightSearchService flightSearchService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FlightOfferDto> search(
            @RequestParam(required = false) String origin,
            @RequestParam String destination,
            @RequestParam(required = false) String departDate,
            @RequestParam(required = false) String returnDate,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        log.info("[flight] 검색 요청: {} → {} ({}~{})", origin, destination, departDate, returnDate);
        return flightSearchService.search(origin, destination, departDate, returnDate, limit);
    }
}
