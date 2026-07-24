package com.waynai.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * LLM(JSON) 응답이 스키마를 살짝 어겨도 파싱이 통째로 실패하지 않도록 관대화한다.
 *
 * <p>예: days(정수)에 문장을 넣거나, area(객체)에 문자열 "null" 을 넣는 경우 →
 * 예외 대신 해당 필드를 null 로 처리(다른 필드는 정상 파싱). 역직렬화에만 영향, 직렬화는 무관.
 */
@Component
@RequiredArgsConstructor
public class JacksonLenientConfig {

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void relax() {
        // 문자열을 숫자/불리언/POJO 필드에 넣은 경우 → 예외 대신 null.
        objectMapper.coercionConfigFor(LogicalType.Integer)
                .setCoercion(CoercionInputShape.String, CoercionAction.AsNull);
        objectMapper.coercionConfigFor(LogicalType.Float)
                .setCoercion(CoercionInputShape.String, CoercionAction.AsNull);
        objectMapper.coercionConfigFor(LogicalType.Boolean)
                .setCoercion(CoercionInputShape.String, CoercionAction.AsNull);
        objectMapper.coercionConfigFor(LogicalType.POJO)
                .setCoercion(CoercionInputShape.String, CoercionAction.AsNull);
        objectMapper.coercionConfigFor(LogicalType.Collection)
                .setCoercion(CoercionInputShape.String, CoercionAction.AsNull);
    }
}
