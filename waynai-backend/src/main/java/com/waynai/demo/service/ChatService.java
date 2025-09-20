package com.waynai.demo.service;

import com.waynai.demo.client.GeminiApiClient;
import com.waynai.demo.dto.*;
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
 * 채팅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiApiClient geminiApiClient;
    private final PromptLoader promptLoader;
    private final TouristInfoService touristInfoService;
    private final RelatedTouristInfoService relatedTouristInfoService;

    /**
     * 채팅 메시지 처리
     * @param request 채팅 요청
     * @return 스트림 응답
     */
    public Flux<String> processChatMessage(ChatRequestDto request) {
        try {
            log.info("채팅 메시지 처리 시작 - 세션: {}, 메시지: {}", request.getSessionId(), request.getMessage());
            
            // 1. 이전 대화를 멀티턴 형식으로 변환
            String multiturn = formatMultiturnConversation(request.getPreviousMessages());
            
            // 2. 컨텍스트 정보 수집 (지역/키워드 기반)
            String context = collectContext(request);
            
            // 3. 프롬프트 생성 및 변수 치환
            String prompt = createChatPrompt(request.getMessage(), multiturn, context);
            
            // 4. Gemini API 호출
            return geminiApiClient.generateTextStream(prompt)
                    .doOnNext(chunk -> log.info("채팅 스트림 응답: {}", chunk))
                    .doOnComplete(() -> log.info("채팅 메시지 처리 완료 - 세션: {}", request.getSessionId()))
                    .doOnError(error -> log.error("채팅 메시지 처리 오류 - 세션: {}", request.getSessionId(), error));
                    
        } catch (Exception e) {
            log.error("채팅 메시지 처리 실패", e);
            return Flux.just("죄송합니다. 채팅 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 이전 대화를 멀티턴 형식으로 변환
     * @param previousMessages 이전 메시지 목록
     * @return 멀티턴 대화 문자열
     */
    private String formatMultiturnConversation(List<ChatMessageDto> previousMessages) {
        if (previousMessages == null || previousMessages.isEmpty()) {
            return "이전 대화가 없습니다.";
        }

        return previousMessages.stream()
                .map(msg -> String.format("%s: %s", 
                    "user".equals(msg.getRole()) ? "사용자" : "상담사", 
                    msg.getContent()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 컨텍스트 정보 수집
     * @param request 채팅 요청
     * @return 컨텍스트 문자열
     */
    private String collectContext(ChatRequestDto request) {
        StringBuilder context = new StringBuilder();
        
        try {
            // 지역 기반 관광지 정보 수집
            if (request.getAreaCode() != null && request.getSigunguCode() != null) {
                TouristSpotResponseDto touristResponse = touristInfoService.getTouristSpots(
                    request.getAreaCode(), request.getSigunguCode(), 1, 5);
                
                if (touristResponse.isSuccess() && touristResponse.getItems() != null) {
                    context.append("=== 관광지 정보 ===\n");
                    context.append(formatTouristSpotsContext(touristResponse.getItems()));
                    context.append("\n\n");
                }
            }
            
            // 키워드 기반 연관 관광지 정보 수집
            if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
                RelatedTouristSpotResponseDto relatedResponse = relatedTouristInfoService.searchKeywordRelatedSpots(
                    request.getAreaCode() != null ? request.getAreaCode() : "11",
                    request.getSigunguCode() != null ? request.getSigunguCode() : "11530",
                    request.getKeyword(), 1, 5);
                
                if (relatedResponse.isSuccess() && relatedResponse.getItems() != null) {
                    context.append("=== 연관 관광지 정보 ===\n");
                    context.append(formatRelatedTouristSpotsContext(relatedResponse.getItems()));
                    context.append("\n\n");
                }
            }
            
            // 컨텍스트가 없으면 기본 메시지
            if (context.length() == 0) {
                context.append("참고자료가 없습니다.");
            }
            
        } catch (Exception e) {
            log.warn("컨텍스트 수집 중 오류 발생", e);
            context.append("참고자료 수집 중 오류가 발생했습니다.");
        }
        
        return context.toString();
    }

    /**
     * 채팅 프롬프트 생성
     * @param query 사용자 질의
     * @param multiturn 이전 대화
     * @param context 컨텍스트
     * @return 완성된 프롬프트
     */
    private String createChatPrompt(String query, String multiturn, String context) {
        String prompt = promptLoader.getPrompt("chat");
        if (prompt == null) {
            throw new RuntimeException("채팅 프롬프트를 찾을 수 없습니다.");
        }

        Map<String, String> variables = new HashMap<>();
        variables.put("query", query);
        variables.put("multiturn", multiturn);
        variables.put("context", context);

        return promptLoader.getPromptWithVariables("chat", variables);
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
                    "---",
                    spot.getHubTatsNm(),
                    spot.getAreaNm(), spot.getAreaCd(),
                    spot.getSignguNm(), spot.getSignguCd(),
                    spot.getHubCtgryLclsNm(),
                    spot.getHubCtgryMclsNm(),
                    spot.getHubCtgrySclsNm() != null ? spot.getHubCtgrySclsNm() : "정보없음",
                    spot.getHubRank()
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
                    "기준 관광지: %s\n" +
                    "연관 관광지: %s\n" +
                    "연관 지역: %s %s\n" +
                    "연관 시군구: %s %s\n" +
                    "연관 대분류: %s\n" +
                    "연관 중분류: %s\n" +
                    "연관 소분류: %s\n" +
                    "---",
                    spot.getTAtsNm(),
                    spot.getRlteTatsNm(),
                    spot.getRlteRegnNm(), spot.getRlteRegnCd(),
                    spot.getRlteSignguNm(), spot.getRlteSignguCd(),
                    spot.getRlteCtgryLclsNm(),
                    spot.getRlteCtgryMclsNm(),
                    spot.getRlteCtgrySclsNm()
                ))
                .collect(Collectors.joining("\n"));
    }
}
