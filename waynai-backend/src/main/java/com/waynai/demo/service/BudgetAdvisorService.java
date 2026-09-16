package com.waynai.demo.service;

import com.waynai.demo.dto.BudgetAssessmentDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.util.BudgetParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 예산 대비 경비 비교 + 절감 제안 (PLAN 2.5 잔여 2건).
 *
 * <p><b>LLM 을 부르지 않는다.</b> 이 단계는 계획 생성 경로에 동기로 끼므로 모델을 부르면 비용과
 * 지연이 그대로 사용자에게 더해진다. 게다가 절감액은 회차마다 흔들리면 안 되는 숫자다 —
 * {@link TravelOrchestratorService#computeCosts} 가 이미 규칙 기반으로 만든 항목별 금액에
 * <b>산술만</b> 적용한다.
 *
 * <p>절감률은 추정치다. 그래서 제안마다 {@code basis}(어떤 비율을 어디에 곱했는지)를 함께 실어
 * 사용자가 숫자를 그대로 믿지 않도록 한다.
 */
@Slf4j
@Service
public class BudgetAdvisorService {

    /** 예산 안에 들었다고 말하려면 이만큼은 남아야 한다. 그 아래는 TIGHT(빠듯). */
    private static final double WITHIN_MARGIN = 0.95;

    /** 절감률 — 근거는 각 제안의 basis 문구에 그대로 적는다. */
    private static final double RATE_STAY = 0.30;
    private static final double RATE_FOOD = 0.25;
    private static final double RATE_TRANSPORT_DOMESTIC = 0.40;
    private static final double RATE_TRANSPORT_ABROAD = 0.30;
    private static final double RATE_ACTIVITIES = 0.30;
    private static final double RATE_FLIGHTS = 0.15;

    /** 숙소를 더 낮출 여지가 없다고 보는 1박 요금(원, 물가계수 적용 전). */
    private static final int STAY_FLOOR_KRW = 60_000;

    /** 한 번에 보여주는 제안 최대 개수. */
    private static final int MAX_SUGGESTIONS = 5;

    /**
     * 여행 조건 — {@code computeCosts} 가 이미 계산해 둔 값을 그대로 받는다.
     * 여기서 다시 추정하면 총액과 절감액이 서로 다른 전제 위에 서게 된다.
     */
    public record TripCost(int days, int nights, int party, int rooms, int perNightKrw,
                           double coef, boolean domestic,
                           int foodPerDayKrw, int activPerDayKrw, int localTransDayKrw) { }

    /**
     * @param plan   총액·항목별 비용이 이미 채워진 계획
     * @param cost   계획을 만든 전제(인원·박수 등)
     * @param budget 사용자가 말한 예산. null 이면 비교 없이 절감 제안만 낸다.
     * @return 평가 결과. 총액이 없으면 null(비교할 것이 없다).
     */
    public BudgetAssessmentDto assess(TravelPlanDto plan, TripCost cost, BudgetParser.Budget budget) {
        if (plan == null || cost == null) return null;
        int estimated = plan.getEstimatedBudgetKrw() != null ? plan.getEstimatedBudgetKrw() : 0;
        if (estimated <= 0) {
            log.info("[budget] 총 예상 경비가 없어 예산 비교를 건너뜁니다.");
            return null;
        }

        Integer budgetTotal = null;
        String basis = null;
        Boolean perPerson = null;
        if (budget != null) {
            perPerson = budget.perPerson();
            budgetTotal = budget.perPerson()
                    ? budget.amountKrw() * Math.max(1, cost.party())
                    : budget.amountKrw();
            basis = budget.perPerson()
                    ? String.format("\"%s\" × %d인 = %,d원", budget.raw(), cost.party(), budgetTotal)
                    : String.format("\"%s\" (여행 전체 기준)", budget.raw());
        }

        List<BudgetAssessmentDto.Saving> pool = buildSavings(plan, cost);
        BudgetAssessmentDto.Status status = classify(estimated, budgetTotal);
        int gap = budgetTotal != null ? estimated - budgetTotal : 0;
        List<BudgetAssessmentDto.Saving> picked = pick(pool, status, gap, plan, cost);

        int totalSaving = picked.stream()
                .map(BudgetAssessmentDto.Saving::getSavingKrw)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        int projected = Math.max(0, estimated - totalSaving);

        return BudgetAssessmentDto.builder()
                .status(status)
                .budgetKrw(budgetTotal)
                .perPerson(perPerson)
                .budgetBasis(basis)
                .estimatedKrw(estimated)
                .diffKrw(budgetTotal != null ? gap : null)
                .ratio(budgetTotal != null && budgetTotal > 0
                        ? Math.round(((double) estimated / budgetTotal) * 100) / 100.0 : null)
                .message(message(status, estimated, budgetTotal, totalSaving, projected, picked.size()))
                .savings(picked)
                .totalSavingKrw(totalSaving)
                .projectedKrw(projected)
                .stillOver(budgetTotal != null ? projected > budgetTotal : null)
                .build();
    }

    private BudgetAssessmentDto.Status classify(int estimated, Integer budgetTotal) {
        if (budgetTotal == null || budgetTotal <= 0) return BudgetAssessmentDto.Status.UNKNOWN;
        double ratio = (double) estimated / budgetTotal;
        if (ratio > 1.0) return BudgetAssessmentDto.Status.OVER;
        if (ratio > WITHIN_MARGIN) return BudgetAssessmentDto.Status.TIGHT;
        return BudgetAssessmentDto.Status.WITHIN;
    }

    /** 항목별 절감 후보. 여행을 줄이는 '일정 단축' 은 여기 넣지 않는다(마지막 수단이라 따로 붙인다). */
    private List<BudgetAssessmentDto.Saving> buildSavings(TravelPlanDto plan, TripCost c) {
        List<BudgetAssessmentDto.Saving> out = new ArrayList<>();
        TravelPlanDto.CostBreakdown cb = plan.getCostBreakdown();
        if (cb == null) return out;

        int stay = nz(cb.getAccommodationKrw());
        int stayFloor = (int) Math.round(STAY_FLOOR_KRW * c.coef());
        if (stay > 0 && c.perNightKrw() > stayFloor) {
            int saving = round1000(stay * RATE_STAY);
            int newNight = round1000(c.perNightKrw() * (1 - RATE_STAY));
            out.add(saving("accommodation", "숙소 등급을 한 단계 낮추기",
                    String.format("1박 %,d원 → %,d원(게스트하우스·모텔·에어비앤비급)으로 바꾸면 %d박 × %d실 기준.",
                            c.perNightKrw(), newNight, c.nights(), c.rooms()),
                    saving,
                    String.format("숙박비 %,d원의 %.0f%%", stay, RATE_STAY * 100)));
        }

        int food = nz(cb.getFoodKrw());
        if (food > 0) {
            int saving = round1000(food * RATE_FOOD);
            out.add(saving("food", "하루 한 끼를 가볍게",
                    String.format("한 끼를 현지 시장·분식·편의점으로 바꾸면 1인 1일 %,d원 → %,d원.",
                            c.foodPerDayKrw(), round1000(c.foodPerDayKrw() * (1 - RATE_FOOD))),
                    saving,
                    String.format("식비 %,d원의 %.0f%%", food, RATE_FOOD * 100)));
        }

        int transport = nz(cb.getTransportKrw());
        if (transport > 0) {
            double rate = c.domestic() ? RATE_TRANSPORT_DOMESTIC : RATE_TRANSPORT_ABROAD;
            out.add(saving("transport",
                    c.domestic() ? "렌터카·택시 대신 대중교통" : "교통 패스·1일권 활용",
                    c.domestic()
                            ? "지하철·시내버스·시외버스 위주로 동선을 짜면 현지 교통비가 줄어듭니다."
                            : "현지 교통 패스(1일권·주간권)를 쓰면 개별 승차권보다 저렴합니다.",
                    round1000(transport * rate),
                    String.format("현지 교통비 %,d원의 %.0f%%", transport, rate * 100)));
        }

        int activities = nz(cb.getActivitiesKrw());
        if (activities > 0) {
            out.add(saving("activities", "유료 관광지 한 곳을 무료 명소로",
                    "전망대·박물관 한 곳을 공원·해안길·무료 전시로 바꾸면 입장료가 줄어듭니다.",
                    round1000(activities * RATE_ACTIVITIES),
                    String.format("입장료·액티비티 %,d원의 %.0f%%", activities, RATE_ACTIVITIES * 100)));
        }

        int flights = nz(cb.getFlightsKrw());
        if (flights > 0) {
            out.add(saving("flights", "주중 출발·경유편으로 항공권 낮추기",
                    "같은 노선이라도 주중 출발이나 1회 경유, LCC 를 고르면 요금이 내려갑니다.",
                    round1000(flights * RATE_FLIGHTS),
                    String.format("항공권 %,d원의 %.0f%% (실제 절감폭은 날짜에 따라 다릅니다)",
                            flights, RATE_FLIGHTS * 100)));
        }

        out.sort(Comparator.comparingInt(
                (BudgetAssessmentDto.Saving s) -> s.getSavingKrw() == null ? 0 : s.getSavingKrw()).reversed());
        return out;
    }

    /**
     * 보여줄 제안 선택.
     * <ul>
     *   <li>예산 초과: 절감액 큰 것부터 부족분을 메울 때까지. 그래도 모자라면 마지막으로 '하루 줄이기'.</li>
     *   <li>그 외: 상위 3개만(권하지 않아도 되는 상황에서 목록을 길게 늘어놓지 않는다).</li>
     * </ul>
     */
    private List<BudgetAssessmentDto.Saving> pick(List<BudgetAssessmentDto.Saving> pool,
                                                  BudgetAssessmentDto.Status status, int gap,
                                                  TravelPlanDto plan, TripCost c) {
        if (pool.isEmpty()) return pool;
        if (status != BudgetAssessmentDto.Status.OVER) {
            return new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
        }
        List<BudgetAssessmentDto.Saving> picked = new ArrayList<>();
        int sum = 0;
        for (BudgetAssessmentDto.Saving s : pool) {
            if (picked.size() >= MAX_SUGGESTIONS) break;
            picked.add(s);
            sum += nz(s.getSavingKrw());
            if (sum >= gap) break;
        }
        if (sum < gap && c.days() >= 3 && picked.size() < MAX_SUGGESTIONS) {
            picked.add(shortenTrip(c));
        }
        return picked;
    }

    /** 마지막 수단 — 하루를 줄이면 그날 경비와 그 전날 숙박이 함께 빠진다. */
    private BudgetAssessmentDto.Saving shortenTrip(TripCost c) {
        int ground = (c.foodPerDayKrw() + c.activPerDayKrw()) * c.party() + c.localTransDayKrw();
        int stay = c.perNightKrw() * c.rooms();
        return saving("schedule", "일정을 하루 줄이기",
                String.format("%d일 → %d일로 줄이면 그날 경비(%,d원)와 숙박 1박(%,d원)이 함께 빠집니다.",
                        c.days(), c.days() - 1, ground, stay),
                round1000(ground + stay),
                "하루치 식비·입장료·현지교통 + 1박 숙박");
    }

    private String message(BudgetAssessmentDto.Status status, int estimated, Integer budget,
                           int totalSaving, int projected, int count) {
        return switch (status) {
            case OVER -> {
                int over = estimated - budget;
                String head = String.format("예상 경비가 예산보다 %,d원 많아요 (예산 %,d원 · 예상 %,d원).",
                        over, budget, estimated);
                if (count == 0) yield head + " 줄일 만한 항목을 찾지 못했어요.";
                String body = String.format(" 아래 %d가지를 적용하면 %,d원 줄어 %,d원이 됩니다.",
                        count, totalSaving, projected);
                yield head + body + (projected > budget
                        ? String.format(" 그래도 %,d원이 모자라요.", projected - budget)
                        : " 예산 안에 들어옵니다.");
            }
            case TIGHT -> String.format("예산 %,d원에 거의 맞닿아 있어요 (예상 %,d원, 여유 %,d원).",
                    budget, estimated, budget - estimated);
            case WITHIN -> String.format("예산 %,d원 안에 들어요 (예상 %,d원, %,d원 남음).",
                    budget, estimated, budget - estimated);
            case UNKNOWN -> String.format(
                    "예상 경비는 %,d원이에요. 예산을 함께 알려주시면 비교해 드릴게요.", estimated);
        };
    }

    private BudgetAssessmentDto.Saving saving(String category, String title, String detail,
                                              int savingKrw, String basis) {
        return BudgetAssessmentDto.Saving.builder()
                .category(category).title(title).detail(detail)
                .savingKrw(savingKrw).basis(basis).build();
    }

    /** 사람이 읽는 금액이라 천원 단위로 맞춘다. */
    private static int round1000(double v) {
        return (int) (Math.round(v / 1000.0) * 1000);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }
}
