package com.waynai.demo.client;

import com.waynai.demo.dto.gemini.GeminiRequestDto;
import com.waynai.demo.dto.gemini.GeminiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiApiClient {
    
    private final WebClient webClient;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    public Mono<GeminiResponseDto> generateContent(String prompt) {
        return generateContent(prompt, 0.7, 1000);
    }
    
    public Mono<GeminiResponseDto> generateContent(String prompt, Double temperature, Integer maxTokens) {
        log.info("[Gemini] 요청 프롬프트:\n{}", prompt);
        GeminiRequestDto request = createRequest(prompt, temperature, maxTokens);
        return webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponseDto.class)
                .doOnSuccess(response -> {
                    log.info("[Gemini] 응답 전체: {}", response);
                    String text = extractTextFromResponse(response);
                    log.info("[Gemini] 추출된 텍스트: {}", text);
                })
                .doOnError(error -> log.error("Gemini API request failed: {}", error.getMessage()));
    }
    
    private GeminiRequestDto createRequest(String prompt, Double temperature, Integer maxTokens) {
        GeminiRequestDto.Part part = GeminiRequestDto.Part.builder()
                .text(prompt)
                .build();
        
        GeminiRequestDto.Content content = GeminiRequestDto.Content.builder()
                .parts(Collections.singletonList(part))
                .build();
        
        GeminiRequestDto.GenerationConfig config = GeminiRequestDto.GenerationConfig.builder()
                .temperature(temperature)
                .topK(40)
                .topP(0.95)
                .maxOutputTokens(maxTokens)
                .build();
        
        return GeminiRequestDto.builder()
                .contents(Collections.singletonList(content))
                .generationConfig(config)
                .build();
    }
    
    public String extractTextFromResponse(GeminiResponseDto response) {
        if (response == null || response.getCandidates() == null || response.getCandidates().isEmpty()) {
            log.warn("Empty response from Gemini API");
            return "";
        }
        
        GeminiResponseDto.Candidate candidate = response.getCandidates().get(0);
        if (candidate.getContent() == null || candidate.getContent().getParts() == null || candidate.getContent().getParts().isEmpty()) {
            log.warn("No content parts in Gemini API response");
            return "";
        }
        
        return candidate.getContent().getParts().get(0).getText();
    }
} 