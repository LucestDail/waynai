package com.waynai.demo.controller;

import com.waynai.demo.dto.ChatRequestDto;
import com.waynai.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 채팅 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 채팅 메시지 처리
     * @param request 채팅 요청
     * @return 스트림 응답
     */
    @PostMapping(value = "/message", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> sendMessage(@RequestBody ChatRequestDto request) {
        log.info("채팅 메시지 수신 - 세션: {}, 메시지: {}", request.getSessionId(), request.getMessage());
        return chatService.processChatMessage(request);
    }

    /**
     * 간단한 채팅 메시지 처리 (GET 방식)
     * @param message 사용자 메시지
     * @param sessionId 세션 ID
     * @return 스트림 응답
     */
    @GetMapping(value = "/simple", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> sendSimpleMessage(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId) {
        
        if (sessionId == null) {
            sessionId = "default-session";
        }
        
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(message);
        request.setSessionId(sessionId);
        
        log.info("간단한 채팅 메시지 수신 - 세션: {}, 메시지: {}", sessionId, message);
        return chatService.processChatMessage(request);
    }

    /**
     * 지역 기반 채팅 메시지 처리
     * @param message 사용자 메시지
     * @param sessionId 세션 ID
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @return 스트림 응답
     */
    @GetMapping(value = "/area", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> sendAreaMessage(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        if (sessionId == null) {
            sessionId = "default-session";
        }
        
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(message);
        request.setSessionId(sessionId);
        request.setAreaCode(areaCode);
        request.setSigunguCode(sigunguCode);
        
        log.info("지역 기반 채팅 메시지 수신 - 세션: {}, 메시지: {}, 지역: {}, 시군구: {}", 
                sessionId, message, areaCode, sigunguCode);
        return chatService.processChatMessage(request);
    }

    /**
     * 키워드 기반 채팅 메시지 처리
     * @param message 사용자 메시지
     * @param sessionId 세션 ID
     * @param keyword 검색 키워드
     * @param areaCode 지역 코드
     * @param sigunguCode 시군구 코드
     * @return 스트림 응답
     */
    @GetMapping(value = "/keyword", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> sendKeywordMessage(
            @RequestParam String message,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String sigunguCode) {
        
        if (sessionId == null) {
            sessionId = "default-session";
        }
        
        ChatRequestDto request = new ChatRequestDto();
        request.setMessage(message);
        request.setSessionId(sessionId);
        request.setKeyword(keyword);
        request.setAreaCode(areaCode);
        request.setSigunguCode(sigunguCode);
        
        log.info("키워드 기반 채팅 메시지 수신 - 세션: {}, 메시지: {}, 키워드: {}, 지역: {}, 시군구: {}", 
                sessionId, message, keyword, areaCode, sigunguCode);
        return chatService.processChatMessage(request);
    }
}
