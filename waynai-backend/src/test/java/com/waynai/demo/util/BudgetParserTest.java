package com.waynai.demo.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 자연어에서 예산 금액을 뽑는 규칙 검증.
 *
 * <p>여기서 <b>오탐이 더 위험하다</b> — "2박3일" 의 3 이나 "3만보" 를 예산으로 읽으면
 * 멀쩡한 계획에 "예산 초과" 라는 틀린 판정이 붙는다. 그래서 음성 사례를 양성 사례만큼 잠근다.
 */
class BudgetParserTest {

    // ---------- 양성 ----------

    @Test
    @DisplayName("'예산 100만원' 을 여행 전체 예산으로 읽는다")
    void parsesTotalBudget() {
        BudgetParser.Budget b = BudgetParser.parse("부산 2박3일 커플 여행, 예산 100만원");
        assertNotNull(b);
        assertEquals(1_000_000, b.amountKrw());
        assertFalse(b.perPerson());
        assertEquals("100만원", b.raw());
    }

    @Test
    @DisplayName("'1인당 50만원' 은 1인 기준으로 표시한다")
    void parsesPerPerson() {
        BudgetParser.Budget b = BudgetParser.parse("오사카 3박4일, 1인당 50만원 정도로");
        assertNotNull(b);
        assertEquals(500_000, b.amountKrw());
        assertTrue(b.perPerson(), "인원을 곱해야 하는 값인지 구분되어야 한다");
    }

    @Test
    @DisplayName("쉼표·억·인당 축약형도 읽는다")
    void parsesVariants() {
        assertEquals(1_000_000, BudgetParser.parse("예산 1,000,000원").amountKrw());
        assertEquals(150_000_000, BudgetParser.parse("예산 1.5억원").amountKrw());
        assertEquals(800_000, BudgetParser.parse("예산은 80만 정도").amountKrw(),
                "예산 마커가 있으면 '원' 없이 단위만 있어도 금액으로 본다");
        assertTrue(BudgetParser.parse("인당 40만원이면 충분").perPerson());
        assertEquals(300_000, BudgetParser.parse("30만원 이내로 다녀오고 싶어요").amountKrw());
    }

    @Test
    @DisplayName("금액이 하나뿐이면 예산 마커가 없어도 채택한다")
    void singleAmountIsTheBudget() {
        BudgetParser.Budget b = BudgetParser.parse("제주 2박3일 200만원");
        assertNotNull(b);
        assertEquals(2_000_000, b.amountKrw());
    }

    // ---------- 음성 (오탐 방지) ----------

    @Test
    @DisplayName("기간·인원·날짜·시각의 숫자는 금액이 아니다")
    void ignoresNonMoneyNumbers() {
        assertNull(BudgetParser.parse("부산 2박3일 4인 가족 여행"));
        assertNull(BudgetParser.parse("2026-05-01 출발 3박4일"));
        assertNull(BudgetParser.parse("오전 10시에 출발해서 오후 6시 도착"));
        assertNull(BudgetParser.parse("서울 근교 당일치기 추천해줘"));
    }

    @Test
    @DisplayName("'원' 없이 단위만 붙은 숫자는 예산 마커가 없으면 금액으로 보지 않는다")
    void unitWithoutWonNeedsMarker() {
        // "3만보 걷기" 의 3만을 300,000원으로 읽으면 안 된다.
        assertNull(BudgetParser.parse("제주 올레길 하루 3만보 걷기 코스"));
        assertNull(BudgetParser.parse("5천 계단 오르는 코스"));
    }

    @Test
    @DisplayName("금액이 여럿인데 어느 것이 예산인지 모르면 포기한다")
    void givesUpWhenAmbiguous() {
        // 잘못 고르면 "예산 초과" 라는 틀린 판정이 나온다 — 판정을 안 하는 편이 낫다.
        assertNull(BudgetParser.parse("항공권 30만원, 숙소 15만원짜리 찾아줘"));
    }

    @Test
    @DisplayName("금액이 여럿이면 '예산' 낱말이 앞에 붙은 것을 고른다")
    void strongMarkerWinsOverWeakOne() {
        // "30만원쯤" 에도 약한 어미가 붙어 있지만, 예산은 '예산' 이라고 말한 쪽이다.
        BudgetParser.Budget b = BudgetParser.parse("항공권은 30만원쯤 보는데 전체 예산 150만원으로 부탁해");
        assertNotNull(b);
        assertEquals(1_500_000, b.amountKrw());
        assertEquals("150만원", b.raw());
    }

    @Test
    @DisplayName("한 끼 값처럼 너무 작은 금액은 예산이 아니다")
    void ignoresTooSmall() {
        assertNull(BudgetParser.parse("커피 5000원 하는 카페 알려줘"));
    }

    @Test
    @DisplayName("빈 입력에도 터지지 않는다")
    void handlesBlank() {
        assertNull(BudgetParser.parse(null));
        assertNull(BudgetParser.parse(""));
        assertNull(BudgetParser.parse("   "));
    }
}
