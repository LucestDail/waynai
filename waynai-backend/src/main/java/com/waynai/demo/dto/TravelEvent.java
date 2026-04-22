package com.waynai.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 로 프론트에 보내는 진행 이벤트. 타입별로 payload 의미가 달라집니다.
 *
 * type 목록:
 *  - stage          : 단계 전환 알림 (analyzing | searching | generating | completed | error)
 *  - intent         : 의도 분석 결과
 *  - sources.tour   : 관광공사 RAG 수집 결과 요약
 *  - sources.naver  : 네이버 블로그 RAG 수집 결과 요약
 *  - model          : Gemini 핫스왑 라우터가 최종 선택한 모델명
 *  - token          : AI 본문 청크 (증분 텍스트)
 *  - plan           : 구조화(JSON) 여행 계획 파싱 결과 (TravelPlanDto)
 *  - done           : 전체 완료
 *  - error          : 최종 실패
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelEvent {

    /** 이벤트 타입 (위 주석 참고). */
    private String type;

    /** 전체 진행 단계 힌트. 프론트 스테퍼 바인딩용. */
    private String stage;

    /** 사용자에게 보여줄 짧은 한글 메시지. */
    private String message;

    /** 타입별 페이로드 (직렬화 가능 객체). */
    private Object payload;
}
