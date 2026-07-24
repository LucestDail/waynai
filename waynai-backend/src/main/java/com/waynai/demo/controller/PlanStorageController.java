package com.waynai.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.waynai.demo.service.PlanStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 저장된 여행 계획 파일 저장소 REST API (인증 없음, 단일 사용자용).
 * 프론트 히스토리(localStorage)의 서버 동기화 백엔드.
 */
@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanStorageController {

    private final PlanStorageService storage;

    /** 저장/갱신 → {"id": "..."} */
    @PostMapping
    public ResponseEntity<?> save(@RequestBody JsonNode body) {
        try {
            String id = storage.save(body);
            return ResponseEntity.ok(Map.of("id", id));
        } catch (Exception e) {
            log.warn("[plans] 저장 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** 요약 목록(최신순) */
    @GetMapping
    public ResponseEntity<List<JsonNode>> list() {
        try {
            return ResponseEntity.ok(storage.list());
        } catch (Exception e) {
            log.warn("[plans] 목록 실패: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    /** 단건 전체 */
    @GetMapping("/{id}")
    public ResponseEntity<JsonNode> get(@PathVariable String id) {
        try {
            JsonNode n = storage.get(id);
            return n == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(n);
        } catch (Exception e) {
            log.warn("[plans] 조회 실패 {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            boolean removed = storage.delete(id);
            return ResponseEntity.ok(Map.of("deleted", removed));
        } catch (Exception e) {
            log.warn("[plans] 삭제 실패 {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
