package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 예산 대비 경비 비교 + 절감 제안 (PLAN 2.5).
 *
 * <p>총액 산출({@code costBreakdown}·{@code estimatedBudgetKrw})은 이미 있었고, 이 DTO 는 그 위에
 * <b>사용자가 말한 예산과의 비교</b>와 <b>줄일 수 있는 항목</b>을 얹는다.
 *
 * <p>전부 <b>규칙 기반 산술</b>이라 LLM 을 부르지 않는다 — 계획 생성 경로에 동기로 끼는 단계라
 * 여기서 모델을 부르면 비용과 지연이 그대로 더해진다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetAssessmentDto {

    /** 예산 대비 판정. */
    public enum Status {
        /** 예산 안에 든다(여유 5% 초과). */
        WITHIN,
        /** 예산에 거의 맞닿았다(초과하지는 않음). */
        TIGHT,
        /** 예산을 넘는다. */
        OVER,
        /** 사용자가 예산을 말하지 않아 비교할 수 없다(절감 제안만 제공). */
        UNKNOWN
    }

    private Status status;

    /** 사용자가 말한 예산(원, 여행 전체 기준으로 환산된 값). 없으면 null. */
    private Integer budgetKrw;
    /** 예산을 1인 기준으로 말했는지(그 경우 budgetKrw 는 인원을 곱한 값이다). */
    private Boolean perPerson;
    /** 예산 해석 근거 — 화면·로그에서 "왜 이 숫자인가" 를 볼 수 있게. */
    private String budgetBasis;

    /** 산출된 총 예상 경비(원). {@code TravelPlanDto.estimatedBudgetKrw} 와 같은 값. */
    private Integer estimatedKrw;
    /** 예상 경비 − 예산. 음수면 남는 돈. 예산이 없으면 null. */
    private Integer diffKrw;
    /** 예상 경비 / 예산. 예산이 없으면 null. */
    private Double ratio;

    /** 사람이 읽는 한 줄 요약. */
    private String message;

    /** 절감 제안(절감액 큰 순). */
    private List<Saving> savings;

    /** 제안을 모두 적용했을 때 줄어드는 총액(원). */
    private Integer totalSavingKrw;
    /** 제안을 모두 적용한 뒤의 예상 경비(원). */
    private Integer projectedKrw;
    /** 제안을 다 적용해도 예산을 넘는지. 예산이 없으면 null. */
    private Boolean stillOver;

    /** 절감 제안 한 건. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Saving {
        /** 항목 (accommodation | food | transport | activities | flights | schedule). */
        private String category;
        /** 제목 (예: "숙소 등급을 한 단계 낮추기"). */
        private String title;
        /** 구체적 설명 — 무엇을 무엇으로 바꾸는지. */
        private String detail;
        /** 절감액(원). */
        private Integer savingKrw;
        /** 산출 근거(어떤 비율을 어디에 곱했는지). 숫자를 그대로 믿지 않도록 함께 보여준다. */
        private String basis;
    }
}
