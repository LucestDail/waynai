package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.client.DaeroClient;
import com.waynai.demo.client.GeocodingClient;
import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.client.HotelCrawlClient;
import com.waynai.demo.client.IataResolver;
import com.waynai.demo.client.RoutingApiClient;
import com.waynai.demo.client.TavilyApiClient;
import com.waynai.demo.dto.BudgetAssessmentDto;
import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.TravelEvent;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.PromptLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 배선 검증 — {@link BudgetAdvisorService} 가 <b>실제로 파이프라인에서 불리는지</b>.
 *
 * <p>순수 클래스만 테스트하면 "로직은 맞는데 안 불린다" 를 못 잡는다. 평가 호출 한 줄을 지워도
 * {@code BudgetAdvisorServiceTest} 는 전부 초록불이다. 그래서 여기서는
 * ① 계획에 붙었는지 ② SSE {@code budget} 이벤트가 나갔는지 ③ 총액 계산의 전제(인원·박수)를
 * 그대로 물려받았는지를 본다.
 */
class TravelOrchestratorBudgetWiringTest {

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
                mock(IataResolver.class),
                mock(TavilyApiClient.class),
                mock(HotelCrawlClient.class),
                mock(GeocodingClient.class),
                mock(DaeroClient.class),
                new BudgetAdvisorService());
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

    private TravelPlanDto planWithDays(int days) {
        List<TravelPlanDto.DayPlan> it = new ArrayList<>();
        for (int i = 1; i <= days; i++) it.add(TravelPlanDto.DayPlan.builder().day(i).build());
        return TravelPlanDto.builder().destination("부산").days(days).itinerary(it).build();
    }

    /** computeCosts → assessBudget 을 파이프라인과 같은 순서로 태운다. */
    private List<TravelEvent> runCostAndBudget(TravelPlanDto plan, IntentAnalysisDto intent,
                                               String query, Integer budgetKrw) {
        TravelOrchestratorService o = newOrchestrator();
        BudgetAdvisorService.TripCost cost = invoke(o, "computeCosts",
                new Class[]{TravelPlanDto.class, IntentAnalysisDto.class, List.class},
                plan, intent, List.of());
        assertNotNull(cost, "비용 전제가 없으면 예산 평가도 없다");

        Sinks.Many<TravelEvent> sink = Sinks.many().replay().all();
        invoke(o, "assessBudget",
                new Class[]{TravelPlanDto.class, BudgetAdvisorService.TripCost.class,
                        String.class, Integer.class, Sinks.Many.class},
                plan, cost, query, budgetKrw, sink);
        sink.tryEmitComplete();

        List<TravelEvent> events = new ArrayList<>();
        sink.asFlux().subscribe(events::add);
        return events;
    }

    @Test
    @DisplayName("질의 문장의 예산이 계획에 붙고 budget 이벤트로도 나간다")
    void attachesAssessmentAndEmitsEvent() {
        TravelPlanDto plan = planWithDays(2);
        IntentAnalysisDto intent = IntentAnalysisDto.builder()
                .destination("부산").days(2).companions("커플").build();

        List<TravelEvent> events = runCostAndBudget(plan, intent, "부산 1박2일 커플, 예산 30만원", null);

        BudgetAssessmentDto a = plan.getBudgetAssessment();
        assertNotNull(a, "평가가 계획에 붙지 않으면 프론트·저장본 어디에도 남지 않는다");
        assertEquals(300_000, a.getBudgetKrw());
        assertEquals(plan.getEstimatedBudgetKrw(), a.getEstimatedKrw(),
                "총액과 비교 기준이 다르면 화면에 두 숫자가 어긋나 보인다");

        assertEquals(1, events.stream().filter(e -> "budget".equals(e.getType())).count(),
                "budget 이벤트가 정확히 한 번 나가야 한다: "
                        + events.stream().map(TravelEvent::getType).toList());
        TravelEvent evt = events.get(0);
        assertSame(a, evt.getPayload());
        assertNotNull(evt.getMessage());
    }

    @Test
    @DisplayName("예산을 파라미터로 주면 문장보다 우선한다")
    void explicitBudgetWins() {
        TravelPlanDto plan = planWithDays(2);
        IntentAnalysisDto intent = IntentAnalysisDto.builder().destination("부산").days(2).build();

        runCostAndBudget(plan, intent, "부산 1박2일, 예산 30만원", 900_000);

        assertEquals(900_000, plan.getBudgetAssessment().getBudgetKrw(),
                "명시 파라미터가 있는데 문장을 읽으면 사용자가 고친 값이 무시된다");
    }

    @Test
    @DisplayName("예산을 말하지 않아도 절감 제안은 붙는다")
    void noBudgetStillAttaches() {
        TravelPlanDto plan = planWithDays(2);
        IntentAnalysisDto intent = IntentAnalysisDto.builder().destination("부산").days(2).build();

        runCostAndBudget(plan, intent, "부산 1박2일 여행 추천해줘", null);

        BudgetAssessmentDto a = plan.getBudgetAssessment();
        assertNotNull(a);
        assertEquals(BudgetAssessmentDto.Status.UNKNOWN, a.getStatus());
        assertFalse(a.getSavings().isEmpty());
    }

    @Test
    @DisplayName("1인당 예산은 computeCosts 가 센 인원을 그대로 쓴다")
    void perPersonUsesSameParty() {
        // "가족" → computeCosts 는 4인으로 센다. 절감 제안이 다른 인원을 쓰면 두 숫자가 어긋난다.
        TravelPlanDto plan = planWithDays(3);
        IntentAnalysisDto intent = IntentAnalysisDto.builder()
                .destination("부산").days(3).companions("가족").build();

        runCostAndBudget(plan, intent, "부산 2박3일 가족여행, 1인당 30만원", null);

        assertEquals(1_200_000, plan.getBudgetAssessment().getBudgetKrw(), "30만원 × 4인");
        assertTrue(plan.getBudgetAssessment().getBudgetBasis().contains("× 4인"));
    }
}
