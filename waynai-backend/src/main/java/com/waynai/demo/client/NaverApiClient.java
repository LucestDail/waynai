package com.waynai.demo.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.NaverBlogSearchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 네이버 블로그 검색 API 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaverApiClient {

    @Value("${naver.api.client.id}")
    private String clientId;

    @Value("${naver.api.client.secret}")
    private String clientSecret;

    @Value("${naver.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper;

    /**
     * 네이버 블로그 검색
     * @param query 검색어
     * @param display 검색 결과 개수 (기본값: 10, 최댓값: 100)
     * @param start 검색 시작 위치 (기본값: 1, 최댓값: 1000)
     * @param sort 정렬 방법 (sim: 정확도순, date: 날짜순)
     * @return 네이버 블로그 검색 결과
     */
    public NaverBlogSearchDto searchBlog(String query, Integer display, Integer start, String sort) {
        try {
            log.info("네이버 블로그 검색 시작: query={}, display={}, start={}, sort={}", 
                    query, display, start, sort);

            // 기본값 설정
            if (display == null) display = 10;
            if (start == null) start = 1;
            if (sort == null) sort = "sim";

            // 검색어 URL 인코딩
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            
            // API URL 구성
            String fullApiUrl = String.format("%s?query=%s&display=%d&start=%d&sort=%s", 
                    apiUrl, encodedQuery, display, start, sort);

            // HTTP 헤더 설정
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("X-Naver-Client-Id", clientId);
            requestHeaders.put("X-Naver-Client-Secret", clientSecret);

            // API 호출
            String responseBody = get(fullApiUrl, requestHeaders);
            
            // JSON 응답을 DTO로 변환
            NaverBlogSearchDto result = objectMapper.readValue(responseBody, NaverBlogSearchDto.class);
            
            log.info("네이버 블로그 검색 완료: 총 {}개 결과", result.getTotal());
            return result;

        } catch (Exception e) {
            log.error("네이버 블로그 검색 실패", e);
            throw new RuntimeException("네이버 블로그 검색 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 네이버 블로그 검색 (기본 파라미터)
     * @param query 검색어
     * @return 네이버 블로그 검색 결과
     */
    public NaverBlogSearchDto searchBlog(String query) {
        return searchBlog(query, 10, 1, "sim");
    }

    /**
     * HTTP GET 요청 실행
     * @param apiUrl API URL
     * @param requestHeaders 요청 헤더
     * @return 응답 본문
     */
    private String get(String apiUrl, Map<String, String> requestHeaders) {
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 오류 발생
                String errorBody = readBody(con.getErrorStream());
                log.error("API 오류 응답: code={}, body={}", responseCode, errorBody);
                throw new RuntimeException("API 오류: " + responseCode + " - " + errorBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    /**
     * HTTP 연결 생성
     * @param apiUrl API URL
     * @return HttpURLConnection
     */
    private HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    /**
     * 응답 본문 읽기
     * @param body 입력 스트림
     * @return 응답 본문 문자열
     */
    private String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);

        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는 데 실패했습니다.", e);
        }
    }
}
