package com.waynai.demo.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.JsonSchemaLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 구조화 출력 스키마({@code schema/travel_plan.schema.json}) 검증.
 *
 * <p>이 스키마는 <b>깨져도 조용하다</b> — 엔드포인트가 거부하면 라우터가 json_object 로 강등하고
 * 계획은 그대로 나온다. 그래서 "스키마를 쓰고 있다" 고 믿은 채 사실은 안 쓰는 상태가 될 수 있다.
 * 두 가지를 잠근다.
 *
 * <ol>
 *   <li><b>strict 규격</b>: OpenAI/OpenRouter 구조화 출력은 모든 객체가
 *       {@code additionalProperties:false} 이고 {@code required} 가 {@code properties} 전체여야 한다.
 *       하나라도 어기면 호출이 400 으로 튕긴다.</li>
 *   <li><b>DTO 와의 정합</b>: 스키마에만 있고 {@link TravelPlanDto} 에 없는 키는 파싱 때 버려진다
 *       (모델이 그 키를 채우느라 토큰만 쓴다). 필드명을 바꾸면 여기서 깨진다.</li>
 * </ol>
 *
 * <p>검사기 자신이 일하는지도 확인한다 — 일부러 망가뜨린 스키마 세 가지를 같은 검사기에 먹여
 * 전부 걸리는 것을 본다(안 걸리면 "검사하지 않은 것" 이 "통과" 로 보인다).
 */
class TravelPlanSchemaTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode schema() {
        JsonNode s = new JsonSchemaLoader().load("travel_plan");
        assertNotNull(s, "travel_plan.schema.json 을 못 읽으면 구조화 출력이 조용히 꺼진다");
        return s;
    }

    // ---------- 검사기 ----------

    /** strict 규격 위반 목록. 비어 있어야 통과. */
    private static List<String> strictViolations(JsonNode node, String path) {
        List<String> out = new ArrayList<>();
        if (node == null || !node.isObject()) return out;

        if ("object".equals(node.path("type").asText())) {
            if (!node.path("additionalProperties").isBoolean() || node.path("additionalProperties").asBoolean()) {
                out.add(path + ": additionalProperties 가 false 가 아니다");
            }
            Set<String> props = new LinkedHashSet<>();
            node.path("properties").fieldNames().forEachRemaining(props::add);
            Set<String> required = new LinkedHashSet<>();
            node.path("required").forEach(r -> required.add(r.asText()));
            if (!props.equals(required)) {
                Set<String> missing = new LinkedHashSet<>(props);
                missing.removeAll(required);
                Set<String> extra = new LinkedHashSet<>(required);
                extra.removeAll(props);
                out.add(path + ": required 가 properties 와 다르다 (빠짐=" + missing + ", 군더더기=" + extra + ")");
            }
            node.path("properties").fields().forEachRemaining(e ->
                    out.addAll(strictViolations(e.getValue(), path + "." + e.getKey())));
        }
        if ("array".equals(node.path("type").asText())) {
            out.addAll(strictViolations(node.path("items"), path + "[]"));
        }
        return out;
    }

    /** 스키마에 등장하는 모든 프로퍼티 이름. */
    private static Set<String> schemaProperties(JsonNode node) {
        Set<String> out = new LinkedHashSet<>();
        if (node == null || !node.isObject()) return out;
        node.path("properties").fields().forEachRemaining(e -> {
            out.add(e.getKey());
            out.addAll(schemaProperties(e.getValue()));
        });
        if (node.has("items")) out.addAll(schemaProperties(node.path("items")));
        return out;
    }

    /** TravelPlanDto 와 그 중첩 클래스의 필드명 전부. */
    private static Set<String> dtoFieldNames() {
        Set<String> out = new HashSet<>();
        List<Class<?>> classes = new ArrayList<>();
        classes.add(TravelPlanDto.class);
        classes.addAll(List.of(TravelPlanDto.class.getDeclaredClasses()));
        for (Class<?> c : classes) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.isSynthetic()) out.add(f.getName());
            }
        }
        return out;
    }

    // ---------- 본 검증 ----------

    @Test
    @DisplayName("strict 규격을 지킨다 — 모든 객체가 additionalProperties:false 이고 required=properties")
    void schemaIsStrictCompliant() {
        List<String> violations = strictViolations(schema(), "$");
        assertTrue(violations.isEmpty(), "strict 위반: " + violations);
    }

    @Test
    @DisplayName("검사 대상이 비어 있지 않다 — 패스가 바뀌어 0건을 통과로 읽는 것을 막는다")
    void schemaIsNotEmpty() {
        JsonNode s = schema();
        Set<String> props = schemaProperties(s);
        assertTrue(props.size() >= 40, "스키마 프로퍼티가 " + props.size() + "개뿐이다 — 스키마가 비었거나 탐색이 안 된 것");
        assertTrue(props.contains("itinerary") && props.contains("costBreakdown") && props.contains("spots"),
                "핵심 키가 빠졌다: " + props);
    }

    @Test
    @DisplayName("스키마의 모든 키가 TravelPlanDto 에 실재한다 — 없는 키는 파싱에서 버려진다")
    void schemaKeysExistOnDto() {
        Set<String> unknown = new LinkedHashSet<>(schemaProperties(schema()));
        unknown.removeAll(dtoFieldNames());
        assertTrue(unknown.isEmpty(), "DTO 에 없는 키: " + unknown);
    }

    @Test
    @DisplayName("스키마대로 온 응답은 TravelPlanDto 로 그대로 파싱된다")
    void schemaShapedResponseParses() throws Exception {
        String sample = """
                {"type":"travel_plan","destination":"부산","duration":"1박 2일","days":2,
                 "summary":"요약","theme":"바다","transportation":"KTX",
                 "accommodation":{"name":"호텔","area":"해운대","type":"호텔","pricePerNightKrw":120000,"bookingUrl":""},
                 "itinerary":[{"day":1,"title":"1일차","overview":"개요",
                   "spots":[{"name":"해운대","visitTime":"09:00","durationMin":60,"activity":"산책",
                             "notes":"","address":"부산","latitude":35.15,"longitude":129.16}],
                   "transportation":"도보","meals":[{"type":"점심","name":"식당","location":"해운대","menu":"밀면","priceKrw":9000}],
                   "accommodation":"호텔","weather":"맑음","estimatedCost":"약 10만원",
                   "costItems":[{"label":"식비","krw":30000}],"tips":"팁"}],
                 "budget":"약 50만원","estimatedBudgetKrw":500000,"tips":["팁"],
                 "weatherInfo":"온화","localInfo":"치안 양호","packingList":["우산"],
                 "costBreakdown":{"flightsKrw":0,"accommodationKrw":120000,"foodKrw":160000,
                   "transportKrw":30000,"activitiesKrw":80000,"etcKrw":39000}}
                """;
        TravelPlanDto plan = MAPPER.readValue(sample, TravelPlanDto.class);
        assertEquals("부산", plan.getDestination());
        assertEquals(1, plan.getItinerary().size());
        assertEquals(35.15, plan.getItinerary().get(0).getSpots().get(0).getLatitude());
        assertEquals(9000, plan.getItinerary().get(0).getMeals().get(0).getPriceKrw());
        assertEquals(120000, plan.getCostBreakdown().getAccommodationKrw());
    }

    // ---------- 검사기 생존 확인 (변이) ----------

    @Test
    @DisplayName("일부러 망가뜨린 스키마는 검사기가 전부 잡는다")
    void checkerCatchesMutations() {
        // ① required 에서 하나 빼기
        ObjectNode a = schema().deepCopy();
        ((com.fasterxml.jackson.databind.node.ArrayNode) a.get("required")).remove(0);
        assertFalse(strictViolations(a, "$").isEmpty(), "required 누락을 못 잡았다");

        // ② 중첩 객체의 additionalProperties 지우기
        ObjectNode b = schema().deepCopy();
        ((ObjectNode) b.path("properties").path("accommodation")).remove("additionalProperties");
        assertFalse(strictViolations(b, "$").isEmpty(), "중첩 객체의 additionalProperties 누락을 못 잡았다");

        // ③ 배열 items 안쪽(itinerary[].spots[]) 망가뜨리기 — 깊은 곳까지 도는지
        ObjectNode c = schema().deepCopy();
        ((ObjectNode) c.path("properties").path("itinerary").path("items")
                .path("properties").path("spots").path("items")).put("additionalProperties", true);
        assertFalse(strictViolations(c, "$").isEmpty(), "배열 안쪽까지 검사하지 못했다");

        // ④ DTO 에 없는 키 추가
        ObjectNode d = schema().deepCopy();
        ((ObjectNode) d.path("properties")).putObject("존재하지않는필드").put("type", "string");
        Set<String> unknown = new LinkedHashSet<>(schemaProperties(d));
        unknown.removeAll(dtoFieldNames());
        assertEquals(Set.of("존재하지않는필드"), unknown, "DTO 에 없는 키를 못 잡았다");
    }
}
