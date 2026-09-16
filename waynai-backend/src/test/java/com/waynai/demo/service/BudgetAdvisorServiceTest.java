package com.waynai.demo.service;

import com.waynai.demo.dto.BudgetAssessmentDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.BudgetParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 예산 대비 경비 비교 + 절감 제안 검증 (PLAN 2.5).
 *
 * <p>전제: 총액과 절감액이 <b>같은 근거 위에 서 있어야 한다</b>. 절감액을 따로 추정하면
 * "150만원짜리 여행에서 200만원을 아낄 수 있다" 같은 값이 나온다.
 */
class BudgetAdvisorServiceTest {

    private final BudgetAdvisorService advisor = new BudgetAdvisorService();

    /** 국내 2인 1박2일 — computeCosts 기본 규칙과 같은 모양의 값. */
    private BudgetAdvisorService.TripCost domesticTrip() {
        return new BudgetAdvisorService.TripCost(
                2, 1, 2, 1, 120_000, 1.0, true, 40_000, 20_000, 15_000);
    }

    private TravelPlanDto planWith(int flights, int stay, int food, int transport, int activities, int etc) {
        int total = flights + stay + food + transport + activities + etc;
        return TravelPlanDto.builder()
                .destination("부산")
                .days(2)
                .estimatedBudgetKrw(total)
                .costBreakdown(TravelPlanDto.CostBreakdown.builder()
                        .flightsKrw(flights).accommodationKrw(stay).foodKrw(food)
                        .transportKrw(transport).activitiesKrw(activities).etcKrw(etc)
                        .build())
                .build();
    }

    /** 합계 570,000원짜리 국내 여행. */
    private TravelPlanDto samplePlan() {
        return planWith(0, 120_000, 160_000, 30_000, 80_000, 39_000);
    }

    // ---------- 비교 ----------

    @Test
    @DisplayName("예산 안에 들면 WITHIN 과 남는 금액을 알려준다")
    void withinBudget() {
        var a = advisor.assess(samplePlan(), domesticTrip(), new BudgetParser.Budget(1_000_000, false, "100만원"));

        assertEquals(BudgetAssessmentDto.Status.WITHIN, a.getStatus());
        assertEquals(1_000_000, a.getBudgetKrw());
        assertEquals(429_000, a.getEstimatedKrw(), "총액은 계획 값 그대로");
        assertEquals(-571_000, a.getDiffKrw(), "남는 돈은 음수로 나온다");
        assertFalse(a.getStillOver());
        assertTrue(a.getMessage().contains("안에 들어요"));
    }

    @Test
    @DisplayName("예산에 거의 맞닿으면 TIGHT — 초과는 아니지만 여유가 없다고 말한다")
    void tightBudget() {
        // 예상 429,000 / 예산 440,000 → 97.5%
        var a = advisor.assess(samplePlan(), domesticTrip(), new BudgetParser.Budget(440_000, false, "44만원"));
        assertEquals(BudgetAssessmentDto.Status.TIGHT, a.getStatus());
        assertTrue(a.getDiffKrw() < 0, "초과는 아니다");
    }

    @Test
    @DisplayName("예산을 넘으면 OVER + 초과액 + 적용 후 금액을 함께 낸다")
    void overBudget() {
        var a = advisor.assess(samplePlan(), domesticTrip(), new BudgetParser.Budget(300_000, false, "30만원"));

        assertEquals(BudgetAssessmentDto.Status.OVER, a.getStatus());
        assertEquals(129_000, a.getDiffKrw());
        assertTrue(a.getRatio() > 1.0);
        assertFalse(a.getSavings().isEmpty(), "초과인데 제안이 없으면 쓸모가 없다");
        assertEquals(a.getEstimatedKrw() - a.getTotalSavingKrw(), a.getProjectedKrw(),
                "적용 후 금액은 총액 − 절감액이어야 한다");
        assertNotNull(a.getStillOver());
    }

    @Test
    @DisplayName("1인당 예산은 인원을 곱해 비교하고 그 근거를 남긴다")
    void perPersonBudgetIsMultiplied() {
        var a = advisor.assess(samplePlan(), domesticTrip(), new BudgetParser.Budget(300_000, true, "30만원"));

        assertEquals(600_000, a.getBudgetKrw(), "2인 여행이므로 30만원 × 2");
        assertTrue(a.getPerPerson());
        assertTrue(a.getBudgetBasis().contains("× 2인"), "근거: " + a.getBudgetBasis());
        assertEquals(BudgetAssessmentDto.Status.WITHIN, a.getStatus());
    }

    @Test
    @DisplayName("예산을 말하지 않아도 절감 제안은 낸다(UNKNOWN)")
    void unknownBudgetStillSuggests() {
        var a = advisor.assess(samplePlan(), domesticTrip(), null);

        assertEquals(BudgetAssessmentDto.Status.UNKNOWN, a.getStatus());
        assertNull(a.getBudgetKrw());
        assertNull(a.getDiffKrw());
        assertNull(a.getRatio());
        assertNull(a.getStillOver());
        assertFalse(a.getSavings().isEmpty(), "비교만 못 할 뿐 줄일 항목은 같다");
    }

    // ---------- 절감 제안 ----------

    @Test
    @DisplayName("절감액은 항목별 비용에서 나온다 — 근거와 금액이 서로 맞는다")
    void savingsComeFromBreakdown() {
        var a = advisor.assess(samplePlan(), domesticTrip(), null);

        var food = a.getSavings().stream().filter(s -> "food".equals(s.getCategory())).findFirst().orElseThrow();
        assertEquals(40_000, food.getSavingKrw(), "식비 160,000원의 25%");
        assertTrue(food.getBasis().contains("25%"), "근거: " + food.getBasis());

        for (var s : a.getSavings()) {
            assertTrue(s.getSavingKrw() > 0, s.getCategory() + " 절감액이 0이면 제안이 아니다");
            assertNotNull(s.getDetail());
            assertNotNull(s.getBasis(), "근거 없는 금액은 그대로 믿게 된다");
        }
    }

    @Test
    @DisplayName("절감 합계가 총액을 넘지 않는다")
    void savingsNeverExceedTotal() {
        var a = advisor.assess(samplePlan(), domesticTrip(), new BudgetParser.Budget(50_000, false, "5만원"));
        assertTrue(a.getTotalSavingKrw() <= a.getEstimatedKrw(),
                "절감액이 총액보다 크면 전제가 어긋난 것이다");
        assertTrue(a.getProjectedKrw() >= 0);
    }

    @Test
    @DisplayName("이미 싼 숙소는 '등급 낮추기' 를 권하지 않는다")
    void doesNotSuggestCheaperStayWhenAlreadyCheap() {
        var cheap = new BudgetAdvisorService.TripCost(
                2, 1, 2, 1, 40_000, 1.0, true, 40_000, 20_000, 15_000);
        var plan = planWith(0, 40_000, 160_000, 30_000, 80_000, 31_000);

        var a = advisor.assess(plan, cheap, null);
        assertTrue(a.getSavings().stream().noneMatch(s -> "accommodation".equals(s.getCategory())),
                "40,000원짜리 숙소를 더 낮추라고 하면 현실성이 없다");
    }

    @Test
    @DisplayName("'일정 하루 줄이기' 는 다른 제안으로 부족할 때만 마지막에 붙는다")
    void shortenTripIsLastResort() {
        var trip = new BudgetAdvisorService.TripCost(
                4, 3, 2, 1, 120_000, 1.0, true, 40_000, 20_000, 15_000);
        var plan = TravelPlanDto.builder().days(4).estimatedBudgetKrw(990_000)
                .costBreakdown(TravelPlanDto.CostBreakdown.builder()
                        .flightsKrw(0).accommodationKrw(360_000).foodKrw(320_000)
                        .transportKrw(60_000).activitiesKrw(160_000).etcKrw(90_000).build())
                .build();

        // 넉넉한 예산 → 일정은 건드리지 않는다.
        var easy = advisor.assess(plan, trip, new BudgetParser.Budget(2_000_000, false, "200만원"));
        assertTrue(easy.getSavings().stream().noneMatch(s -> "schedule".equals(s.getCategory())));
        assertTrue(easy.getSavings().size() <= 3, "권할 필요가 없을 때 목록을 늘어놓지 않는다");

        // 항목 절감을 다 해도 못 맞추는 예산 → 마지막에 일정 단축.
        var hard = advisor.assess(plan, trip, new BudgetParser.Budget(300_000, false, "30만원"));
        var last = hard.getSavings().get(hard.getSavings().size() - 1);
        assertEquals("schedule", last.getCategory(), "여행을 줄이는 제안은 맨 뒤여야 한다");
        assertTrue(hard.getStillOver(), "그래도 모자라면 모자란다고 말해야 한다");
    }

    @Test
    @DisplayName("해외 여행은 항공권 제안이 붙고 교통 제안 문구가 달라진다")
    void abroadDiffers() {
        var abroad = new BudgetAdvisorService.TripCost(
                4, 3, 2, 1, 160_000, 1.35, false, 54_000, 27_000, 20_000);
        var plan = planWith(1_000_000, 480_000, 432_000, 80_000, 216_000, 220_000);

        var a = advisor.assess(plan, abroad, null);
        List<String> cats = a.getSavings().stream().map(BudgetAssessmentDto.Saving::getCategory).toList();
        assertTrue(cats.contains("flights"), "항공권이 가장 큰 덩어리인데 빠졌다: " + cats);
        assertEquals("flights", cats.get(0), "절감액이 큰 것부터 보여야 한다");
    }

    // ---------- 방어 ----------

    @Test
    @DisplayName("총액이 없거나 입력이 비면 평가를 만들지 않는다")
    void handlesMissingInput() {
        assertNull(advisor.assess(null, domesticTrip(), null));
        assertNull(advisor.assess(samplePlan(), null, null));
        assertNull(advisor.assess(TravelPlanDto.builder().build(), domesticTrip(), null),
                "총액이 없으면 비교할 것이 없다");
    }

    @Test
    @DisplayName("항목별 비용이 없으면 제안 없이 총액만 말한다")
    void handlesMissingBreakdown() {
        var plan = TravelPlanDto.builder().estimatedBudgetKrw(500_000).build();
        var a = advisor.assess(plan, domesticTrip(), new BudgetParser.Budget(300_000, false, "30만원"));

        assertEquals(BudgetAssessmentDto.Status.OVER, a.getStatus());
        assertTrue(a.getSavings().isEmpty());
        assertEquals(0, a.getTotalSavingKrw());
        assertTrue(a.getMessage().contains("찾지 못했"), "줄일 게 없으면 없다고 말해야 한다: " + a.getMessage());
    }
}
