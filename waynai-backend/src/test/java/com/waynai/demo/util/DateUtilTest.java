package com.waynai.demo.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DateUtil} 포맷 로직 — 현재 시각에 의존하므로 형식/일관성으로 검증한다.
 */
class DateUtilTest {

    @Test
    void 현재년월은_YYYYMM_6자리() {
        String ym = DateUtil.getCurrentYearMonth();
        assertThat(ym).hasSize(6).matches("\\d{6}");
        assertThat(ym).isEqualTo(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
    }

    @Test
    void 현재날짜시간은_표준_포맷() {
        assertThat(DateUtil.getCurrentDateTime())
                .matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void 이전달은_현재보다_한달_전() {
        String prev = DateUtil.getPreviousMonth();
        assertThat(prev).hasSize(6).matches("\\d{6}");
        assertThat(prev).isEqualTo(LocalDate.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyyMM")));
    }
}
