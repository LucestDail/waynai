package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.DaeroClient;
import com.waynai.demo.client.GeocodingClient;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.client.HotelCrawlClient;
import com.waynai.demo.client.IataResolver;
import com.waynai.demo.client.RoutingApiClient;
import com.waynai.demo.client.TavilyApiClient;
import com.waynai.demo.dto.FlightOfferDto;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.PromptLoader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link TravelOrchestratorService} 의 순수 보정 로직 검증 —
 * 비용 규칙화(computeCosts), 물가계수(costCoef), 좌표 환각 판별(haversineKm),
 * JSON 추출/파싱(extractJson·tryParsePlan), 일수 분배(distributeDays),
 * 항공 추정(estimateIntlRoundTripKrw), 인원 추정(detectParty), intent 국내외 보정(normalizeIntent).
 *
 * <p>이 메서드들은 private 이지만 오케스트레이터의 핵심 도메인 로직(LLM 환각 교정)이므로 리플렉션으로 직접 검증한다.
 * 순수 계산에는 의존성이 필요없어 대부분 mock 은 미사용이며, normalizeIntent 만 IataResolver 를 스텁한다.
 */
class TravelOrchestratorServiceTest {

    private final IataResolver iataResolver = mock(IataResolver.class);

    /** 13개 의존성 중 objectMapper 만 실객체, 나머지는 mock 으로 오케스트레이터 구성. */
    private TravelOrchestratorService newOrchestrator() {
        return new TravelOrchestratorService(
                mock(IntentAnalysisService.class),
                mock(NaverSearchService.class),
                mock(TouristInfoService.class),
                mock(GeminiApiClient.class),
                mock(PromptLoader.class),
                new ObjectMapper(),
                mock(FlightSearchService.class),
                mock(RoutingApiClient.class),
                iataResolver,
                mock(TavilyApiClient.class),
                mock(HotelCrawlClient.class),
                mock(GeocodingClient.class),
                mock(DaeroClient.class));
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(Object target, String name, Class<?>[] types, Object... args) {
        try {
            Method m = TravelOrchestratorService.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            return (T) m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException("리플렉션 호출 실패: " + name, e);
        }
    }

    // ---------- computeCosts: 규칙 기반 비용 현실화 ----------

    private TravelPlanDto planWithDays(int days) {
        List<TravelPlanDto.DayPlan> it = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            it.add(TravelPlanDto.DayPlan.builder().day(i).build());
        }
        return TravelPlanDto.builder().destination("부산").days(days).itinerary(it).build();
    }

    @Test
    void computeCosts_국내_기본2인_합계정합_그리고_항공0() {
        TravelOrchestratorService orch = newOrchestrator();
        IntentAnalysisDto intent = IntentAnalysisDto.builder().destination("부산").build();
        TravelPlanDto plan = planWithDays(3);

        invoke(orch, "computeCosts",
                new Class[]{TravelPlanDto.class, IntentAnalysisDto.class, List.class},
                plan, intent, List.of());

        TravelPlanDto.CostBreakdown cb = plan.getCostBreakdown();
        assertThat(cb).isNotNull();
        assertThat(cb.getFlightsKrw()).isZero(); // 국내·항공권 없음
        int sum = cb.getFlightsKrw() + cb.getAccommodationKrw() + cb.getFoodKrw()
                + cb.getTransportKrw() + cb.getActivitiesKrw() + cb.getEtcKrw();
        assertThat(plan.getEstimatedBudgetKrw()).isEqualTo(sum); // 총액 = 항목합
        assertThat(plan.getBudget()).contains("2인");
        // 일자별 비용이 계산값으로 채워지고 LLM 항목은 제거됨
        assertThat(plan.getItinerary()).allSatisfy(d -> {
            assertThat(d.getEstimatedCost()).contains("원");
            assertThat(d.getCostItems()).isNull();
        });
    }

    @Test
    void computeCosts_해외_실항공권이면_최저가x인원() {
        TravelOrchestratorService orch = newOrchestrator();
        IntentAnalysisDto intent = IntentAnalysisDto.builder()
                .destination("오사카").international(true).build();
        TravelPlanDto plan = planWithDays(3);
        List<FlightOfferDto> flights = List.of(
                FlightOfferDto.builder().price(300000).build(),
                FlightOfferDto.builder().price(250000).build()); // 최저가 250000

        invoke(orch, "computeCosts",
                new Class[]{TravelPlanDto.class, IntentAnalysisDto.class, List.class},
                plan, intent, flights);

        assertThat(plan.getCostBreakdown().getFlightsKrw()).isEqualTo(250000 * 2); // 2인
        assertThat(plan.getBudget()).doesNotContain("추정치"); // 실값이므로 추정 표기 없음
    }

    @Test
    void computeCosts_해외_항공권없으면_추정치사용_그리고_라벨표기() {
        TravelOrchestratorService orch = newOrchestrator();
        IntentAnalysisDto intent = IntentAnalysisDto.builder()
                .destination("파리").international(true).build();
        TravelPlanDto plan = planWithDays(4);

        invoke(orch, "computeCosts",
                new Class[]{TravelPlanDto.class, IntentAnalysisDto.class, List.class},
                plan, intent, List.of());

        // 유럽 왕복 추정 1,300,000 * 2인
        assertThat(plan.getCostBreakdown().getFlightsKrw()).isEqualTo(1300000 * 2);
        assertThat(plan.getBudget()).contains("추정치");
    }

    @Test
    void computeCosts_LLM비현실_1박요금은_기본값으로_교체_그리고_추정표시() {
        TravelOrchestratorService orch = newOrchestrator();
        IntentAnalysisDto intent = IntentAnalysisDto.builder().destination("부산").build();
        TravelPlanDto plan = planWithDays(2);
        // 통화혼동/자리표시자 의심되는 저가(1박 100원)
        plan.setAccommodation(TravelPlanDto.Accommodation.builder()
                .name("무슨호텔").pricePerNightKrw(100).build());

        invoke(orch, "computeCosts",
                new Class[]{TravelPlanDto.class, IntentAnalysisDto.class, List.class},
                plan, intent, List.of());

        // 15000 미만 → 계수기반 기본값(국내 120000)으로 대체 + 추정 플래그
        assertThat(plan.getAccommodation().getPricePerNightKrw()).isEqualTo(120000);
        assertThat(plan.getAccommodation().getPriceEstimated()).isTrue();
    }

    // ---------- costCoef: 목적지 물가계수 ----------

    private double costCoef(IntentAnalysisDto intent) {
        return invoke(newOrchestrator(), "costCoef", new Class[]{IntentAnalysisDto.class}, intent);
    }

    @Test
    void costCoef_물가권역_매핑() {
        assertThat(costCoef(IntentAnalysisDto.builder().destination("스위스").build())).isEqualTo(1.7);
        assertThat(costCoef(IntentAnalysisDto.builder().destination("도쿄").build())).isEqualTo(1.35);
        assertThat(costCoef(IntentAnalysisDto.builder().destination("다낭").build())).isEqualTo(0.6);
        assertThat(costCoef(IntentAnalysisDto.builder().destination("인도").build())).isEqualTo(0.45);
        assertThat(costCoef(IntentAnalysisDto.builder().destination("부산").build())).isEqualTo(1.0); // 국내 기준
        assertThat(costCoef(null)).isEqualTo(1.0);
    }

    // ---------- haversineKm: 좌표 거리(환각 판별 기반) ----------

    @Test
    void haversineKm_서울부산_약325km() {
        double km = invoke(newOrchestrator(), "haversineKm",
                new Class[]{double.class, double.class, double.class, double.class},
                37.5665, 126.9780, 35.1796, 129.0756);
        assertThat(km).isBetween(300.0, 340.0);
    }

    @Test
    void haversineKm_동일좌표는_0() {
        double km = invoke(newOrchestrator(), "haversineKm",
                new Class[]{double.class, double.class, double.class, double.class},
                37.5, 127.0, 37.5, 127.0);
        assertThat(km).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001));
    }

    // ---------- extractJson / tryParsePlan ----------

    @Test
    void extractJson_코드펜스와_스마트따옴표_정규화() {
        TravelOrchestratorService orch = newOrchestrator();
        String fenced = "```json\n{\"a\":1}\n```";
        assertThat((String) invoke(orch, "extractJson", new Class[]{String.class}, fenced))
                .isEqualTo("{\"a\":1}");
        // 스마트따옴표 → 표준따옴표
        String smart = "{“a”:1}";
        assertThat((String) invoke(orch, "extractJson", new Class[]{String.class}, smart))
                .isEqualTo("{\"a\":1}");
    }

    @Test
    void extractJson_JSON없으면_null() {
        TravelOrchestratorService orch = newOrchestrator();
        assertThat((String) invoke(orch, "extractJson", new Class[]{String.class}, "설명만 있고 JSON 없음")).isNull();
        assertThat((String) invoke(orch, "extractJson", new Class[]{String.class}, (Object) null)).isNull();
    }

    @Test
    void tryParsePlan_유효JSON은_DTO_비유효는_null() {
        TravelOrchestratorService orch = newOrchestrator();
        TravelPlanDto ok = invoke(orch, "tryParsePlan", new Class[]{String.class},
                "{\"destination\":\"부산\",\"days\":3}");
        assertThat(ok).isNotNull();
        assertThat(ok.getDestination()).isEqualTo("부산");

        assertThat((TravelPlanDto) invoke(orch, "tryParsePlan", new Class[]{String.class}, "그냥 텍스트")).isNull();
        assertThat((TravelPlanDto) invoke(orch, "tryParsePlan", new Class[]{String.class}, (Object) null)).isNull();
    }

    // ---------- distributeDays ----------

    @Test
    void distributeDays_균등분배_합계보존() {
        TravelOrchestratorService orch = newOrchestrator();
        List<IntentAnalysisDto.Segment> segs = List.of(
                IntentAnalysisDto.Segment.builder().title("A").build(),
                IntentAnalysisDto.Segment.builder().title("B").build(),
                IntentAnalysisDto.Segment.builder().title("C").build());
        int[] out = invoke(orch, "distributeDays", new Class[]{int.class, List.class}, 7, segs);
        assertThat(out).hasSize(3);
        assertThat(java.util.Arrays.stream(out).sum()).isEqualTo(7); // 합계=총일수
        for (int d : out) assertThat(d).isGreaterThanOrEqualTo(1);
    }

    @Test
    void distributeDays_권역일수_가중치반영() {
        TravelOrchestratorService orch = newOrchestrator();
        List<IntentAnalysisDto.Segment> segs = List.of(
                IntentAnalysisDto.Segment.builder().title("A").days(2).build(),
                IntentAnalysisDto.Segment.builder().title("B").days(3).build());
        int[] out = invoke(orch, "distributeDays", new Class[]{int.class, List.class}, 5, segs);
        assertThat(java.util.Arrays.stream(out).sum()).isEqualTo(5);
        assertThat(out[1]).isGreaterThanOrEqualTo(out[0]); // 가중치 큰 권역이 더 많거나 같음
    }

    // ---------- estimateIntlRoundTripKrw ----------

    @Test
    void estimateIntlRoundTrip_권역별_추정() {
        TravelOrchestratorService orch = newOrchestrator();
        assertThat((int) invoke(orch, "estimateIntlRoundTripKrw", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().destination("도쿄").build())).isEqualTo(350000);
        assertThat((int) invoke(orch, "estimateIntlRoundTripKrw", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().destination("방콕").build())).isEqualTo(500000);
        assertThat((int) invoke(orch, "estimateIntlRoundTripKrw", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().destination("파리").build())).isEqualTo(1300000);
        assertThat((int) invoke(orch, "estimateIntlRoundTripKrw", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().destination("뉴욕").build())).isEqualTo(1500000);
        assertThat((int) invoke(orch, "estimateIntlRoundTripKrw", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().destination("어딘가미분류").build())).isEqualTo(900000);
    }

    // ---------- detectParty ----------

    @Test
    void detectParty_동반유형별_인원() {
        TravelOrchestratorService orch = newOrchestrator();
        assertThat((int) invoke(orch, "detectParty", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().companions("혼자").build())).isEqualTo(1);
        assertThat((int) invoke(orch, "detectParty", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().companions("가족").build())).isEqualTo(4);
        assertThat((int) invoke(orch, "detectParty", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().companions("단체").build())).isEqualTo(6);
        assertThat((int) invoke(orch, "detectParty", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().companions("커플").build())).isEqualTo(2);
        assertThat((int) invoke(orch, "detectParty", new Class[]{IntentAnalysisDto.class},
                IntentAnalysisDto.builder().build())).isEqualTo(2); // 기본 2인
    }

    // ---------- needsDomesticFlight ----------

    @Test
    void needsDomesticFlight_도서원거리만_true() {
        TravelOrchestratorService orch = newOrchestrator();
        assertThat((boolean) invoke(orch, "needsDomesticFlight", new Class[]{String.class}, "제주")).isTrue();
        assertThat((boolean) invoke(orch, "needsDomesticFlight", new Class[]{String.class}, "울릉도")).isTrue();
        assertThat((boolean) invoke(orch, "needsDomesticFlight", new Class[]{String.class}, "부산")).isFalse();
        assertThat((boolean) invoke(orch, "needsDomesticFlight", new Class[]{String.class}, (Object) null)).isFalse();
    }

    // ---------- addDays ----------

    @Test
    void addDays_전체날짜와_월단위_그리고_실패시원본() {
        TravelOrchestratorService orch = newOrchestrator();
        // 3일 여행 → depart + (3-1)일
        assertThat((String) invoke(orch, "addDays", new Class[]{String.class, int.class}, "2026-03-10", 3))
                .isEqualTo("2026-03-12");
        // YYYY-MM → 01일 기준
        assertThat((String) invoke(orch, "addDays", new Class[]{String.class, int.class}, "2026-03", 3))
                .isEqualTo("2026-03-03");
        // 파싱 불가 → 원본 반환
        assertThat((String) invoke(orch, "addDays", new Class[]{String.class, int.class}, "bad-date", 3))
                .isEqualTo("bad-date");
    }

    // ---------- normalizeIntent: IATA 기반 국내/해외 결정적 보정 ----------

    @Test
    void normalizeIntent_해외도시면_international_true_그리고_area제거() {
        TravelOrchestratorService orch = newOrchestrator();
        when(iataResolver.resolve("도쿄")).thenReturn("TYO"); // 해외 IATA
        IntentAnalysisDto intent = IntentAnalysisDto.builder()
                .destination("도쿄")
                .area(IntentAnalysisDto.AreaInfo.builder().name("서울").code("11").build())
                .build();

        invoke(orch, "normalizeIntent", new Class[]{IntentAnalysisDto.class, String.class}, intent, "도쿄 여행");

        assertThat(intent.getInternational()).isTrue();
        assertThat(intent.getArea()).isNull(); // 해외 판정 → 국내 지역코드 오염 제거
    }

    @Test
    void normalizeIntent_국내공항이면_international_false() {
        TravelOrchestratorService orch = newOrchestrator();
        when(iataResolver.resolve("부산")).thenReturn("PUS"); // 국내 IATA
        IntentAnalysisDto intent = IntentAnalysisDto.builder().destination("부산").build();

        invoke(orch, "normalizeIntent", new Class[]{IntentAnalysisDto.class, String.class}, intent, "부산 여행");

        assertThat(intent.getInternational()).isFalse();
    }

    @Test
    void normalizeIntent_해석불가면_원본유지() {
        TravelOrchestratorService orch = newOrchestrator();
        when(iataResolver.resolve(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        IntentAnalysisDto intent = IntentAnalysisDto.builder()
                .destination("알수없는곳").international(true).build();

        invoke(orch, "normalizeIntent", new Class[]{IntentAnalysisDto.class, String.class}, intent, "알수없는곳 여행");

        // IATA 해석 실패 → 기존 값 유지
        assertThat(intent.getInternational()).isTrue();
    }
}
