package com.waynai.demo.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * RestTemplate 설정 클래스
 * 외부 API 호출을 위한 RestTemplate 빈 설정
 * SSL 인증서 검증 문제 해결을 위한 설정 포함
 * 
 * 주의: 이 설정은 한국 관광공사 API의 SSL 인증서 문제를 해결하기 위해
 * 호스트명 검증을 비활성화합니다. 프로덕션 환경에서는 더 안전한 방법을 고려해야 합니다.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        // SSL 컨텍스트 생성 (모든 인증서 신뢰)
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(new TrustAllStrategy())
                .build();

        // SSL 연결 소켓 팩토리 생성
        // 한국 관광공사 API의 SSL 인증서 문제를 해결하기 위해 호스트명 검증 비활성화
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.3"},
                null,
                (hostname, session) -> {
                    // apis.data.go.kr 도메인에 대해서만 호스트명 검증을 우회
                    if (hostname != null && hostname.contains("apis.data.go.kr")) {
                        return true;
                    }
                    // 다른 도메인에 대해서는 기본 검증 수행
                    return false;
                }
        );

        // HTTP 클라이언트 생성
        //
        // ⚠️ 응답 대기 상한(responseTimeout)은 여기서 줘야 한다.
        // Spring 6.1 의 HttpComponentsClientHttpRequestFactory 에는 setReadTimeout 이 없어
        // (HttpClient 5 는 소켓 타임아웃을 RequestConfig 로 다룬다) 아래 factory 설정만으로는
        // 연결 수립·풀 대기 시간만 제한되고 **응답 대기는 무한**이 된다.
        // 실제로 공공데이터포털이 느려진 날 myapi 가 같은 이유로 호출자 타임아웃을 넘긴 적이 있다.
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(sslConnectionSocketFactory)
                                .build()
                )
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setResponseTimeout(Timeout.ofSeconds(20))
                                .build()
                )
                .build();

        // HTTP 요청 팩토리 설정
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(java.time.Duration.ofSeconds(10)); // 10초
        factory.setConnectionRequestTimeout(java.time.Duration.ofSeconds(15)); // 15초
        
        return new RestTemplate(factory);
    }
}
