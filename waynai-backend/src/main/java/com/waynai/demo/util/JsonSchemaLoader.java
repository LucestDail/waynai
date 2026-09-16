package com.waynai.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 구조화 출력(JSON Schema) 스키마 로더.
 *
 * <p>{@code classpath:schema/&lt;name&gt;.schema.json} 을 읽어 캐시한다. LLM 요청의
 * {@code response_format.json_schema.schema} 에 그대로 실린다.
 *
 * <p>스키마를 못 읽으면 {@code null} 을 돌려주고 <b>반드시 warn 을 남긴다</b> — 호출부는
 * JSON 모드(json_object)로 조용히 내려가므로, 로그가 없으면 "스키마를 쓰고 있다" 고
 * 착각하게 된다(검사하지 않은 것과 통과한 것을 구분한다).
 */
@Slf4j
@Component
public class JsonSchemaLoader {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonNode> cache = new ConcurrentHashMap<>();

    /** 로드 실패를 캐시에 담기 위한 표식(매 요청마다 파일을 다시 뒤지지 않도록). */
    private static final JsonNode MISSING = new ObjectMapper().createObjectNode();

    /**
     * @param name 확장자 없는 스키마 이름 (예: {@code "travel_plan"})
     * @return 스키마 JsonNode, 없거나 깨졌으면 null
     */
    public JsonNode load(String name) {
        if (name == null || name.isBlank()) return null;
        JsonNode cached = cache.computeIfAbsent(name, this::read);
        return cached == MISSING ? null : cached;
    }

    private JsonNode read(String name) {
        String path = "schema/" + name + ".schema.json";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            JsonNode node = objectMapper.readTree(in);
            if (node == null || !node.isObject()) {
                log.warn("[schema] {} 가 객체가 아니어서 사용하지 않습니다. JSON 모드로 내려갑니다.", path);
                return MISSING;
            }
            log.info("[schema] {} 로드 완료 (구조화 출력 사용)", path);
            return node;
        } catch (Exception e) {
            log.warn("[schema] {} 를 읽지 못했습니다({}). JSON 모드로 내려갑니다.", path, e.getMessage());
            return MISSING;
        }
    }
}
