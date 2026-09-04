package com.waynai.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 서버에 저장된 여행 계획 1건.
 *
 * <p>지금까지 계획은 브라우저 localStorage 에만 있어 기기를 바꾸면 사라졌다.
 * 이 DTO 는 그 저장분을 서버로 옮기기 위한 표현이다.
 *
 * <p><b>개인정보를 담지 않는다.</b> 소유자는 이메일·이름이 아니라 클라이언트가 만든
 * 익명 토큰의 해시로만 구분한다({@code ownerHash}). 계정 체계를 나중에 붙일 때
 * 이 필드를 사용자 식별자로 승격하면 되고, 그전까지는 서버가 사용자가 누구인지 모른다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedPlanDto {

    /** 서버가 발급한 계획 id(UUID). 공유 링크의 주소이기도 하므로 추측 불가해야 한다. */
    private String id;

    /** 소유자 토큰의 SHA-256 해시. 평문 토큰은 서버에 저장하지 않는다. */
    private String ownerHash;

    /** 목록에 보일 제목(목적지 또는 테마). */
    private String title;

    /** 저장 시각(epoch milli). */
    private Long savedAt;

    /** 계획 본문. */
    private TravelPlanDto plan;

    /** 계획에 붙어 있던 항공권 오퍼(있으면). */
    private List<FlightOfferDto> flights;
}
