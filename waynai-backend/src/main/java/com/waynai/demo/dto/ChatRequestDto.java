package com.waynai.demo.dto;

import lombok.Data;

import java.util.List;

/**
 * 채팅 요청 DTO
 */
@Data
public class ChatRequestDto {
    
    private String message; // 사용자 메시지
    private String sessionId; // 채팅 세션 ID
    private String areaCode; // 지역 코드 (선택사항)
    private String sigunguCode; // 시군구 코드 (선택사항)
    private String keyword; // 검색 키워드 (선택사항)
    private List<ChatMessageDto> previousMessages; // 이전 대화 목록
}
