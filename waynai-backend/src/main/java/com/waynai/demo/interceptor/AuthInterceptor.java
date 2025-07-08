package com.waynai.demo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        
        // 현재는 간단한 토큰 검증만 수행
        // 실제 구현에서는 JWT 토큰 검증 등을 수행해야 함
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Unauthorized access attempt: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        
        String token = authHeader.substring(7);
        if (!isValidToken(token)) {
            log.warn("Invalid token provided: {}", token);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        
        log.debug("Authentication successful for URI: {}", request.getRequestURI());
        return true;
    }
    
    private boolean isValidToken(String token) {
        // 실제 구현에서는 JWT 토큰 검증 로직을 구현해야 함
        // 현재는 간단한 예시로 "valid-token"만 허용
        return "valid-token".equals(token);
    }
} 