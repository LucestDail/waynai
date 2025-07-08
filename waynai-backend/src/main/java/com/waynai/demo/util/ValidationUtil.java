package com.waynai.demo.util;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
public class ValidationUtil {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9-+()\\s]+$");
    
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber).matches();
    }
    
    public static boolean isValidLatitude(Double latitude) {
        return latitude != null && latitude >= -90.0 && latitude <= 90.0;
    }
    
    public static boolean isValidLongitude(Double longitude) {
        return longitude != null && longitude >= -180.0 && longitude <= 180.0;
    }
    
    public static boolean isValidDuration(Integer duration) {
        return duration != null && duration > 0 && duration <= 1440; // 최대 24시간 (1440분)
    }
    
    public static boolean isValidRating(Double rating) {
        return rating != null && rating >= 0.0 && rating <= 5.0;
    }
    
    public static boolean isValidDays(Integer days) {
        return days != null && days >= 1 && days <= 30;
    }
    
    public static String sanitizeString(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("[<>\"']", "");
    }
    
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
    
    public static boolean isValidDestination(String destination) {
        return !isNullOrEmpty(destination) && destination.length() <= 100;
    }
    
    public static boolean isValidTheme(String theme) {
        if (isNullOrEmpty(theme)) {
            return false;
        }
        
        String[] validThemes = {"문화", "자연", "맛집", "쇼핑", "역사", "레저", "힐링"};
        for (String validTheme : validThemes) {
            if (validTheme.equals(theme)) {
                return true;
            }
        }
        return false;
    }
} 