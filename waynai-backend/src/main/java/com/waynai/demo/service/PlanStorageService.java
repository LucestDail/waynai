package com.waynai.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 저장된 여행 계획을 파일(JSON)로 물리 저장한다. DB/인증 없음(단일 사용자용).
 *
 * <p>계획 1건 = 파일 1개(`<id>.json`). 본문은 프론트가 보낸 JSON 을 그대로 보관해
 * TravelPlanDto 스키마 변화와 무관하게 동작한다.
 */
@Slf4j
@Service
public class PlanStorageService {

    private final ObjectMapper objectMapper;
    private final Path dir;

    public PlanStorageService(ObjectMapper objectMapper,
                              @Value("${waynai.storage.dir:./data/plans}") String storageDir) {
        this.objectMapper = objectMapper;
        this.dir = Paths.get(storageDir);
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(dir);
            log.info("[plan-storage] 저장 디렉토리: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            log.error("[plan-storage] 저장 디렉토리 생성 실패: {}", e.getMessage());
        }
    }

    /** 계획 저장/갱신. body 는 {id,title,savedAt,plan,flights}. id 없으면 생성해 반환. */
    public String save(JsonNode body) throws IOException {
        String id = body.hasNonNull("id") && !body.get("id").asText().isBlank()
                ? body.get("id").asText() : genId(body);
        ObjectNode node = body.deepCopy();
        node.put("id", id);
        Path file = resolveSafe(id);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
        return id;
    }

    /** 요약 목록(id,title,savedAt) — 본문 제외, 최신순. */
    public List<JsonNode> list() throws IOException {
        List<JsonNode> out = new ArrayList<>();
        if (!Files.exists(dir)) return out;
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> jsons = files.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path p : jsons) {
                try {
                    JsonNode n = objectMapper.readTree(p.toFile());
                    ObjectNode summary = objectMapper.createObjectNode();
                    summary.put("id", n.path("id").asText());
                    summary.put("title", n.path("title").asText());
                    summary.put("savedAt", n.path("savedAt").asLong());
                    out.add(summary);
                } catch (Exception e) {
                    log.warn("[plan-storage] 읽기 실패 {}: {}", p.getFileName(), e.getMessage());
                }
            }
        }
        out.sort(Comparator.comparingLong((JsonNode n) -> n.path("savedAt").asLong()).reversed());
        return out;
    }

    /** 단건 전체 조회. 없으면 null. */
    public JsonNode get(String id) throws IOException {
        Path file = resolveSafe(id);
        if (!Files.exists(file)) return null;
        return objectMapper.readTree(file.toFile());
    }

    /** 삭제. */
    public boolean delete(String id) throws IOException {
        return Files.deleteIfExists(resolveSafe(id));
    }

    private String genId(JsonNode body) {
        // savedAt(있으면) + title 해시로 결정적 id(브라우저 Date.now 규칙과 무관하게 서버에서 생성).
        long ts = body.path("savedAt").asLong(0);
        int h = Math.abs(body.path("title").asText("plan").hashCode());
        return (ts > 0 ? ts : h) + "-" + h;
    }

    /** 경로 조작(../) 방지: id 에서 파일명 문자만 허용. */
    private Path resolveSafe(String id) {
        String safe = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        return dir.resolve(safe + ".json");
    }
}
