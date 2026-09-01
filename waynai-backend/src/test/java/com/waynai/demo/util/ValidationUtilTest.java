package com.waynai.demo.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ValidationUtil} 순수 검증 로직 — 정상/경계/실패 케이스.
 */
class ValidationUtilTest {

    @Test
    void 지역코드_정상_2자리숫자() {
        assertThat(ValidationUtil.isValidAreaCode("01")).isTrue();
        assertThat(ValidationUtil.isValidAreaCode("39")).isTrue();
    }

    @Test
    void 지역코드_비정상_길이나_문자나_null() {
        assertThat(ValidationUtil.isValidAreaCode("1")).isFalse();   // 1자리
        assertThat(ValidationUtil.isValidAreaCode("100")).isFalse(); // 3자리
        assertThat(ValidationUtil.isValidAreaCode("ab")).isFalse();  // 문자
        assertThat(ValidationUtil.isValidAreaCode("")).isFalse();
        assertThat(ValidationUtil.isValidAreaCode(null)).isFalse();
    }

    @Test
    void 시군구코드_정상_5자리숫자() {
        assertThat(ValidationUtil.isValidSigunguCode("11110")).isTrue();
    }

    @Test
    void 시군구코드_비정상() {
        assertThat(ValidationUtil.isValidSigunguCode("1111")).isFalse();   // 4자리
        assertThat(ValidationUtil.isValidSigunguCode("111100")).isFalse(); // 6자리
        assertThat(ValidationUtil.isValidSigunguCode("1111a")).isFalse();
        assertThat(ValidationUtil.isValidSigunguCode(null)).isFalse();
    }

    @Test
    void 페이지번호_경계값() {
        assertThat(ValidationUtil.isValidPageNo(1)).isTrue();
        assertThat(ValidationUtil.isValidPageNo(0)).isFalse();   // 하한 밖
        assertThat(ValidationUtil.isValidPageNo(-1)).isFalse();
        assertThat(ValidationUtil.isValidPageNo(null)).isFalse();
    }

    @Test
    void 페이지당행수_경계값_1부터1000까지() {
        assertThat(ValidationUtil.isValidNumOfRows(1)).isTrue();
        assertThat(ValidationUtil.isValidNumOfRows(1000)).isTrue();   // 상한
        assertThat(ValidationUtil.isValidNumOfRows(1001)).isFalse();  // 상한 초과
        assertThat(ValidationUtil.isValidNumOfRows(0)).isFalse();
        assertThat(ValidationUtil.isValidNumOfRows(null)).isFalse();
    }
}
