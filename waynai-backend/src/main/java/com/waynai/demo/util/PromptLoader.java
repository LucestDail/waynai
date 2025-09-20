package com.waynai.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 프롬프트 로더 유틸리티 클래스
 */
@Slf4j
@Component
public class PromptLoader {

    private final Map<String, String> prompts = new HashMap<>();

    @PostConstruct
    public void loadPrompts() {
        try {
            // 여행 가이드 프롬프트 로드
            loadPrompt("travel_guide", "prompt/travel_guide.txt");
            
            // 관광지 정보 프롬프트 로드
            loadPrompt("tourist_info", "prompt/tourist_info.txt");
            
            // 연관 관광지 프롬프트 로드
            loadPrompt("related_spots", "prompt/related_spots.txt");
            
            // 채팅 프롬프트 로드
            loadPrompt("chat", "prompt/chat.txt");
            
            // 의도 분석 프롬프트 로드
            loadPrompt("intent_analysis", "prompt/intent_analysis.txt");
            
            // 여행 계획 프롬프트 로드
            loadPrompt("travel_plan", "prompt/travel_plan.txt");
            
            log.info("프롬프트 로드 완료: {} 개", prompts.size());
            
        } catch (Exception e) {
            log.error("프롬프트 로드 실패", e);
        }
    }

    private void loadPrompt(String key, String resourcePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourcePath);
        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        prompts.put(key, content);
        log.info("프롬프트 로드 완료: {} -> {}", key, resourcePath);
    }

    /**
     * 프롬프트 조회
     * @param key 프롬프트 키
     * @return 프롬프트 내용
     */
    public String getPrompt(String key) {
        return prompts.get(key);
    }

    /**
     * 프롬프트에 변수 치환 적용
     * @param key 프롬프트 키
     * @param variables 변수 맵
     * @return 치환된 프롬프트
     */
    public String getPromptWithVariables(String key, Map<String, String> variables) {
        String prompt = getPrompt(key);
        if (prompt == null) {
            return null;
        }

        String result = prompt;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "$" + entry.getKey();
            result = result.replace(placeholder, entry.getValue());
        }
        
        return result;
    }

    /**
     * 사용 가능한 프롬프트 키 목록 조회
     * @return 프롬프트 키 목록
     */
    public String[] getAvailablePrompts() {
        return prompts.keySet().toArray(new String[0]);
    }
}
