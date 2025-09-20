package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    
    private String role; // "user" 또는 "assistant"
    private String content; // 메시지 내용
    private LocalDateTime timestamp; // 메시지 시간
    private String sessionId; // 채팅 세션 ID
    
    /**
     * 사용자 메시지 생성
     */
    public static ChatMessageDto userMessage(String content, String sessionId) {
        return ChatMessageDto.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .sessionId(sessionId)
                .build();
    }
    
    /**
     * 어시스턴트 메시지 생성
     */
    public static ChatMessageDto assistantMessage(String content, String sessionId) {
        return ChatMessageDto.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .sessionId(sessionId)
                .build();
    }
}
