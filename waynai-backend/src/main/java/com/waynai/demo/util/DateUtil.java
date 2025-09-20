package com.waynai.demo.util;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 날짜 관련 유틸리티 클래스
 */
public class DateUtil {
    
    private static final DateTimeFormatter YYYY_MM_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final DateTimeFormatter YYYY_MM_DD_HH_MM_SS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 현재 년월을 YYYYMM 형식으로 반환
     * @return 현재 년월 (예: 202503)
     */
    public static String getCurrentYearMonth() {
        return LocalDateTime.now().format(YYYY_MM_FORMATTER);
    }
    
    /**
     * 현재 날짜시간을 YYYY-MM-DD HH:mm:ss 형식으로 반환
     * @return 현재 날짜시간 (예: 2025-03-15 14:30:25)
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(YYYY_MM_DD_HH_MM_SS_FORMATTER);
    }
    
    /**
     * 이전 달의 년월을 YYYYMM 형식으로 반환
     * @return 이전 달 년월 (예: 202502)
     */
    public static String getPreviousMonth() {
        return LocalDate.now().minusMonths(1).format(YYYY_MM_FORMATTER);
    }
}
