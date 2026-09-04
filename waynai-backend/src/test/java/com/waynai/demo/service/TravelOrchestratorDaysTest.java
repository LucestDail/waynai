package com.waynai.demo.service;

import com.waynai.demo.dto.IntentAnalysisDto;
import com.waynai.demo.dto.TravelPlanDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 계획 일수 정합 검증.
 *
 * <p>배경: 단일 도시 경로에 일수 검증이 없어 "부산 2박3일" 요청에 5일 일정이 나오고,
 * {@code duration="2박 3일"} 인데 {@code days=5} 처럼 필드끼리 모순되는 응답까지 나왔다
 * (2026-09-04 실측 5회 중 3회). intent 는 days=3 을 정확히 뽑고 있었으므로 원인은
 * 생성 결과를 검증하지 않은 것이었다.
 */
class TravelOrchestratorDaysTest {

    private static TravelPlanDto planWithDays(int n, Integer declaredDays, String declaredDuration) {
        List<TravelPlanDto.DayPlan> it = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            it.add(TravelPlanDto.DayPlan.builder().day(99).title("day" + i).build());
        }
        return TravelPlanDto.builder()
                .destination("부산")
                .days(declaredDays)
                .duration(declaredDuration)
                .itinerary(it)
                .build();
    }

    private static IntentAnalysisDto wanting(Integer days) {
        return IntentAnalysisDto.builder().destination("부산").days(days).build();
    }

    @Test
    @DisplayName("요청보다 긴 일정은 잘라내고 days·duration 을 맞춘다")
    void trimsLongerItinerary() {
        // 실측된 실패: 2박3일(=3일) 요청에 5일이 나오고 duration 은 "2박 3일" 이었다.
        TravelPlanDto plan = planWithDays(5, 5, "2박 3일");

        TravelOrchestratorService.reconcileDays(plan, wanting(3));

        assertEquals(3, plan.getItinerary().size());
        assertEquals(3, plan.getDays());
        assertEquals("3일", plan.getDuration());
    }

    @Test
    @DisplayName("잘라낸 뒤 day 번호를 1부터 다시 매긴다")
    void renumbersDays() {
        TravelPlanDto plan = planWithDays(4, 4, "3박4일");

        TravelOrchestratorService.reconcileDays(plan, wanting(2));

        assertEquals(List.of(1, 2),
                plan.getItinerary().stream().map(TravelPlanDto.DayPlan::getDay).toList());
    }

    @Test
    @DisplayName("모자란 일정은 지어내지 않되 필드끼리는 맞춘다")
    void doesNotInventMissingDays() {
        // 요청 3일인데 2일치만 생성된 경우. 없는 일정을 만들어내는 것보다 짧게 나가는 편이 낫다.
        TravelPlanDto plan = planWithDays(2, 3, "2박3일");

        TravelOrchestratorService.reconcileDays(plan, wanting(3));

        assertEquals(2, plan.getItinerary().size(), "없는 일정을 채우면 안 된다");
        assertEquals(2, plan.getDays(), "days 는 실제 일정 길이와 같아야 한다");
        assertEquals("2일", plan.getDuration(), "duration 도 실제와 어긋나면 안 된다");
    }

    @Test
    @DisplayName("일수가 맞으면 그대로 두되 days·duration 은 정규화한다")
    void keepsMatchingItinerary() {
        TravelPlanDto plan = planWithDays(3, 3, "2박3일");

        TravelOrchestratorService.reconcileDays(plan, wanting(3));

        assertEquals(3, plan.getItinerary().size());
        assertEquals(3, plan.getDays());
        assertEquals("3일", plan.getDuration());
    }

    @Test
    @DisplayName("intent 에 일수가 없으면 자르지 않고 필드만 맞춘다")
    void noIntentDaysMeansNoTrim() {
        TravelPlanDto plan = planWithDays(4, null, null);

        TravelOrchestratorService.reconcileDays(plan, wanting(null));

        assertEquals(4, plan.getItinerary().size(), "요청 일수가 없으면 모델 결과를 존중한다");
        assertEquals(4, plan.getDays());
        assertEquals("4일", plan.getDuration());
    }

    @Test
    @DisplayName("plan 이 null 이거나 일정이 비어도 터지지 않는다")
    void handlesNullsSafely() {
        assertDoesNotThrow(() -> TravelOrchestratorService.reconcileDays(null, wanting(3)));

        TravelPlanDto empty = TravelPlanDto.builder().itinerary(new ArrayList<>()).build();
        assertDoesNotThrow(() -> TravelOrchestratorService.reconcileDays(empty, wanting(3)));

        TravelPlanDto noItinerary = TravelPlanDto.builder().build();
        assertDoesNotThrow(() -> TravelOrchestratorService.reconcileDays(noItinerary, null));
    }
}
