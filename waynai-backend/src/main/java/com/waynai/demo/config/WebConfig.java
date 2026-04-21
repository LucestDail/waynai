package com.waynai.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정. CORS 허용 도메인은 {@code waynai.cors.allowed-origins} 프로퍼티
 * (ENV: {@code CORS_ALLOWED_ORIGINS})에서 콤마로 구분해 주입합니다.
 *
 * <p>와일드카드(`*`)는 허용하지 않으며, 명시적 도메인만 허용합니다. 개발 기본값은
 * {@code http://localhost:5173}, {@code http://127.0.0.1:5173} 입니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${waynai.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOriginsRaw;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOriginsRaw.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
