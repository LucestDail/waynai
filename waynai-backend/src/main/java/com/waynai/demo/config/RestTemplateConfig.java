package com.waynai.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 설정 클래스
 * 외부 API 호출을 위한 RestTemplate 빈 설정
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(5)); // 5초
        factory.setConnectionRequestTimeout(java.time.Duration.ofSeconds(10)); // 10초
        
        return new RestTemplate(factory);
    }
}
