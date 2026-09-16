package com.waynai.demo.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 자연어 질의에서 <b>여행 예산(원)</b> 을 뽑는다.
 *
 * <p>LLM 을 쓰지 않는다. 의도 분석 LLM 은 이미 {@code budgetLevel}("저렴/보통/고급")만 뽑고 있어
 * 금액 비교에 쓸 수 없고, 금액 추출은 회차마다 흔들려서는 안 되는 값이라 코드로 결정적으로 판정한다.
 *
 * <h3>판정 규칙</h3>
 * <ol>
 *   <li>금액 후보를 전부 찾는다. 후보는 <b>단위(억·천만·만·천)가 있거나 '원' 으로 끝나는</b> 숫자뿐이다
 *       — "2박3일", "4인", "2026-05" 같은 숫자는 후보가 되지 않는다.</li>
 *   <li>후보 앞뒤에 <b>예산 마커</b>(예산·경비·비용·한도·이내·안에서·정도·쓸…)가 있으면 그것을 채택한다.</li>
 *   <li>마커가 없으면 <b>후보가 정확히 하나일 때만</b> 채택한다. 둘 이상이면 어느 것이 예산인지 알 수 없으므로
 *       <b>포기하고 로그를 남긴다</b>(엉뚱한 금액을 예산으로 삼아 "예산 초과" 라고 말하는 쪽이 더 나쁘다).</li>
 * </ol>
 */
@Slf4j
public final class BudgetParser {

    private BudgetParser() { }

    /** 너무 작은 값은 예산이 아니라 한 끼·입장료다. 너무 큰 값은 오타로 본다. */
    private static final long MIN_KRW = 10_000L;
    private static final long MAX_KRW = 1_000_000_000L;

    /** 숫자 + (단위) + (원). 단위나 '원' 중 하나는 반드시 있어야 후보가 된다. */
    private static final Pattern AMOUNT = Pattern.compile(
            "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(억|천만|만|천)?\\s*(원)?");

    /**
     * 금액 <b>앞</b>에 오는 강한 마커. 한국어는 "예산 100만원" 처럼 앞에 붙는다.
     * 뒤쪽까지 뒤지면 다른 금액이 남의 마커를 가져간다("항공권 30만원 … 전체 예산 150만원").
     */
    private static final Pattern STRONG_PREFIX = Pattern.compile("예산|경비|비용|한도|총액");

    /** 금액 <b>뒤</b>에 오는 약한 마커. 예산임을 시사하지만 단정하지는 않는다. */
    private static final Pattern WEAK_SUFFIX = Pattern.compile("이내|안에서|정도|쯤|가지고|들고|으로|까지");

    private static final Pattern PER_PERSON = Pattern.compile("1인당|일인당|인당|1인|한\\s*사람|한사람");

    /**
     * @param amountKrw 원화 금액
     * @param perPerson 1인 기준으로 말했는지
     * @param raw       근거가 된 원문 조각
     */
    public record Budget(int amountKrw, boolean perPerson, String raw) { }

    /** @return 예산, 못 찾거나 모호하면 null */
    public static Budget parse(String query) {
        if (query == null || query.isBlank()) return null;

        List<Budget> candidates = new ArrayList<>();
        List<Budget> strong = new ArrayList<>();
        List<Budget> weak = new ArrayList<>();

        Matcher m = AMOUNT.matcher(query);
        while (m.find()) {
            String unit = m.group(2);
            String won = m.group(3);
            if (unit == null && won == null) continue; // "2박3일", "4인", "2026" 등은 후보가 아니다

            long value = toKrw(m.group(1), unit);
            if (value < MIN_KRW || value > MAX_KRW) continue;

            String raw = query.substring(m.start(), m.end()).trim();
            boolean perPerson = PER_PERSON.matcher(before(query, m.start(), 10)).find();
            Budget b = new Budget((int) value, perPerson, raw);

            boolean hasStrong = STRONG_PREFIX.matcher(before(query, m.start(), 14)).find();
            boolean hasWeak = WEAK_SUFFIX.matcher(after(query, m.end(), 8)).find();
            if (hasStrong) strong.add(b);
            else if (hasWeak) weak.add(b);

            // '원' 없이 단위만 있는 숫자("3만보", "5천 계단")는 돈이 아닐 수 있다.
            // 마커가 곁에 있을 때만 예산으로 인정한다.
            if (won != null) candidates.add(b);
        }

        if (!strong.isEmpty()) return pickFirst(strong, query, "예산 낱말");
        if (!weak.isEmpty()) return pickFirst(weak, query, "예산 어미");
        if (candidates.size() == 1) return candidates.get(0);
        if (candidates.size() > 1) {
            // 조용히 아무거나 고르면 "예산 초과" 라는 틀린 판정이 나온다. 포기하되 이유를 남긴다.
            log.info("[budget] 금액이 {}건인데 예산 마커가 없어 예산을 특정하지 못했습니다: {}",
                    candidates.size(), query);
        }
        return null;
    }

    private static Budget pickFirst(List<Budget> found, String query, String kind) {
        if (found.size() > 1) {
            log.info("[budget] {}이 붙은 금액이 {}건이라 첫 번째({})를 씁니다: {}",
                    kind, found.size(), found.get(0).raw(), query);
        }
        return found.get(0);
    }

    private static long toKrw(String digits, String unit) {
        try {
            double n = Double.parseDouble(digits.replace(",", ""));
            long mult = switch (unit == null ? "" : unit) {
                case "억" -> 100_000_000L;
                case "천만" -> 10_000_000L;
                case "만" -> 10_000L;
                case "천" -> 1_000L;
                default -> 1L;
            };
            return Math.round(n * mult);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String before(String s, int idx, int span) {
        return s.substring(Math.max(0, idx - span), idx);
    }

    private static String after(String s, int idx, int span) {
        return s.substring(idx, Math.min(s.length(), idx + span));
    }
}
