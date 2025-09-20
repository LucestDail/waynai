package com.waynai.demo.service;

import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.TouristSpotDto;
import com.waynai.demo.dto.RelatedTouristSpotDto;
import com.waynai.demo.util.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final GeminiApiClient geminiApiClient;
    private final PromptLoader promptLoader;

    /**
     * 여행 가이드 질의응답
     * @param query 사용자 질의
     * @param context 관광지 정보 컨텍스트
     * @return 스트림 응답
     */
    public Flux<String> askTravelGuide(String query, List<TouristSpotDto> context) {
        String contextText = formatTouristSpotsContext(context);
        return askWithPrompt("travel_guide", query, contextText);
    }

    /**
     * 관광지 정보 질의응답
     * @param query 사용자 질의
     * @param context 관광지 정보 컨텍스트
     * @return 스트림 응답
     */
    public Flux<String> askTouristInfo(String query, List<TouristSpotDto> context) {
        String contextText = formatTouristSpotsContext(context);
        return askWithPrompt("tourist_info", query, contextText);
    }

    /**
     * 연관 관광지 질의응답
     * @param query 사용자 질의
     * @param context 연관 관광지 정보 컨텍스트
     * @return 스트림 응답
     */
    public Flux<String> askRelatedSpots(String query, List<RelatedTouristSpotDto> context) {
        String contextText = formatRelatedTouristSpotsContext(context);
        return askWithPrompt("related_spots", query, contextText);
    }

    /**
     * 일반 질의응답
     * @param query 사용자 질의
     * @return 스트림 응답
     */
    public Flux<String> askGeneral(String query) {
        return geminiApiClient.generateTextStream(query);
    }

    /**
     * 프롬프트를 사용한 질의응답
     * @param promptKey 프롬프트 키
     * @param query 사용자 질의
     * @param context 컨텍스트
     * @return 스트림 응답
     */
    private Flux<String> askWithPrompt(String promptKey, String query, String context) {
        try {
            String prompt = promptLoader.getPrompt(promptKey);
            if (prompt == null) {
                log.error("프롬프트를 찾을 수 없습니다: {}", promptKey);
                return Flux.error(new RuntimeException("프롬프트를 찾을 수 없습니다: " + promptKey));
            }

            Map<String, String> variables = new HashMap<>();
            variables.put("query", query);
            variables.put("context", context);

            return geminiApiClient.generateTextStreamWithVariables(prompt, variables);
            
        } catch (Exception e) {
            log.error("LLM 서비스 오류", e);
            return Flux.error(e);
        }
    }

    /**
     * 관광지 정보를 컨텍스트 텍스트로 변환
     * @param spots 관광지 목록
     * @return 컨텍스트 텍스트
     */
    private String formatTouristSpotsContext(List<TouristSpotDto> spots) {
        if (spots == null || spots.isEmpty()) {
            return "관광지 정보가 없습니다.";
        }

        return spots.stream()
                .map(spot -> String.format(
                    "관광지명: %s\n" +
                    "지역: %s %s\n" +
                    "시군구: %s %s\n" +
                    "대분류: %s\n" +
                    "중분류: %s\n" +
                    "소분류: %s\n" +
                    "순위: %s\n" +
                    "기준년월: %s\n" +
                    "---",
                    spot.getHubTatsNm(),
                    spot.getAreaNm(), spot.getAreaCd(),
                    spot.getSignguNm(), spot.getSignguCd(),
                    spot.getHubCtgryLclsNm(),
                    spot.getHubCtgryMclsNm(),
                    spot.getHubCtgrySclsNm() != null ? spot.getHubCtgrySclsNm() : "정보없음",
                    spot.getHubRank(),
                    spot.getBaseYm()
                ))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 연관 관광지 정보를 컨텍스트 텍스트로 변환
     * @param spots 연관 관광지 목록
     * @return 컨텍스트 텍스트
     */
    private String formatRelatedTouristSpotsContext(List<RelatedTouristSpotDto> spots) {
        if (spots == null || spots.isEmpty()) {
            return "연관 관광지 정보가 없습니다.";
        }

        return spots.stream()
                .map(spot -> String.format(
                    "기준 관광지: %s (%s)\n" +
                    "기준 지역: %s %s\n" +
                    "기준 시군구: %s %s\n" +
                    "연관 관광지: %s (%s)\n" +
                    "연관 지역: %s %s\n" +
                    "연관 시군구: %s %s\n" +
                    "연관 대분류: %s\n" +
                    "연관 중분류: %s\n" +
                    "연관 소분류: %s\n" +
                    "연관 순위: %s\n" +
                    "기준년월: %s\n" +
                    "---",
                    spot.getTAtsNm(), spot.getTAtsCd(),
                    spot.getAreaNm(), spot.getAreaCd(),
                    spot.getSignguNm(), spot.getSignguCd(),
                    spot.getRlteTatsNm(), spot.getRlteTatsCd(),
                    spot.getRlteRegnNm(), spot.getRlteRegnCd(),
                    spot.getRlteSignguNm(), spot.getRlteSignguCd(),
                    spot.getRlteCtgryLclsNm(),
                    spot.getRlteCtgryMclsNm(),
                    spot.getRlteCtgrySclsNm(),
                    spot.getRlteRank(),
                    spot.getBaseYm()
                ))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 사용 가능한 프롬프트 목록 조회
     * @return 프롬프트 목록
     */
    public String[] getAvailablePrompts() {
        return promptLoader.getAvailablePrompts();
    }
}
