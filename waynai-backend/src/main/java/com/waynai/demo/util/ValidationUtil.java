package com.waynai.demo.util;

import java.util.regex.Pattern;

/**
 * 유효성 검증 유틸리티 클래스
 */
public class ValidationUtil {
    
    private static final Pattern AREA_CODE_PATTERN = Pattern.compile("^\\d{2}$");
    private static final Pattern SIGUNGU_CODE_PATTERN = Pattern.compile("^\\d{5}$");
    
    /**
     * 지역 코드 유효성 검증
     * @param areaCode 지역 코드 (2자리 숫자)
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public static boolean isValidAreaCode(String areaCode) {
        return areaCode != null && AREA_CODE_PATTERN.matcher(areaCode).matches();
    }
    
    /**
     * 시군구 코드 유효성 검증
     * @param sigunguCode 시군구 코드 (5자리 숫자)
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public static boolean isValidSigunguCode(String sigunguCode) {
        return sigunguCode != null && SIGUNGU_CODE_PATTERN.matcher(sigunguCode).matches();
    }
    
    /**
     * 페이지 번호 유효성 검증
     * @param pageNo 페이지 번호
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public static boolean isValidPageNo(Integer pageNo) {
        return pageNo != null && pageNo > 0;
    }
    
    /**
     * 페이지당 행 수 유효성 검증
     * @param numOfRows 페이지당 행 수
     * @return 유효한 경우 true, 그렇지 않으면 false
     */
    public static boolean isValidNumOfRows(Integer numOfRows) {
        return numOfRows != null && numOfRows > 0 && numOfRows <= 1000;
    }
}
