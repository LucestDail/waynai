package com.waynai.demo.controller;

import com.waynai.demo.dto.FlightOfferDto;
import com.waynai.demo.dto.SavedPlanDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.service.PlanArchiveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 계획 보관 컨트롤러 — 브라우저 localStorage 에만 있던 저장분을 서버로 옮긴다.
 *
 * <p>소유자는 {@code X-Owner-Token} 헤더로 구분한다. 값은 클라이언트가 한 번 만들어
 * 보관하는 임의 문자열이며 서버는 해시만 저장한다(개인정보 없음).
 *
 * <p>⚠️ 게이트웨이(nginx) 뒤에서 외부 접근은 HTTP Basic 을 쓰므로 <b>Authorization 헤더는
 * 게이트웨이 전용</b>이다. 그래서 소유자 토큰을 전용 헤더로 받는다 — simpleStock 의
 * {@code X-Access-Token}, chominjungum-web 의 {@code X-Auth-Token} 과 같은 이유.
 *
 * <ul>
 *   <li>{@code POST   /api/plans}            저장 → id 반환</li>
 *   <li>{@code GET    /api/plans}            내 목록(최신순, 본문 없음)</li>
 *   <li>{@code GET    /api/plans/{id}}       내 계획 1건(본문 포함)</li>
 *   <li>{@code DELETE /api/plans/{id}}       삭제</li>
 *   <li>{@code GET    /api/plans/{id}/shared} 공유 조회(토큰 불요)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private static final String OWNER_HEADER = "X-Owner-Token";

    private final PlanArchiveService archive;

    /** 저장 요청 본문. */
    @Data
    public static class SaveRequest {
        private TravelPlanDto plan;
        private List<FlightOfferDto> flights;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> save(@RequestHeader(value = OWNER_HEADER, required = false) String ownerToken,
                                  @RequestBody SaveRequest req) {
        if (isBlank(ownerToken)) {
            return missingToken();
        }
        if (req == null || req.getPlan() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "plan 이 없습니다"));
        }
        try {
            SavedPlanDto saved = archive.save(ownerToken, req.getPlan(), req.getFlights());
            // 본문을 되돌려줄 필요가 없다 — 클라이언트가 이미 갖고 있다.
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "title", saved.getTitle(),
                    "savedAt", saved.getSavedAt()));
        } catch (IOException e) {
            log.error("[plans] 저장 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "저장에 실패했습니다"));
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(@RequestHeader(value = OWNER_HEADER, required = false) String ownerToken) {
        if (isBlank(ownerToken)) {
            return missingToken();
        }
        List<SavedPlanDto> mine = archive.list(ownerToken);
        // ownerHash 는 서버 내부 식별자다. 클라이언트는 자기 토큰만 알면 되므로 굳이 내보내지 않는다.
        mine.forEach(p -> p.setOwnerHash(null));
        return ResponseEntity.ok(mine);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> get(@RequestHeader(value = OWNER_HEADER, required = false) String ownerToken,
                                 @PathVariable String id) {
        if (isBlank(ownerToken)) {
            return missingToken();
        }
        return archive.get(ownerToken, id)
                .<ResponseEntity<?>>map(sp -> {
                    sp.setOwnerHash(null); // 목록·공유와 같은 이유로 내부 식별자는 내보내지 않는다
                    return ResponseEntity.ok(sp);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** 공유 링크로 열람. 토큰이 없어도 되지만 id(UUID)를 알아야 한다. */
    @GetMapping(value = "/{id}/shared", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> shared(@PathVariable String id) {
        return archive.getShared(id)
                .<ResponseEntity<?>>map(sp -> {
                    sp.setOwnerHash(null); // 소유자 해시는 공유 응답에서 지운다
                    return ResponseEntity.ok(sp);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = OWNER_HEADER, required = false) String ownerToken,
                                    @PathVariable String id) {
        if (isBlank(ownerToken)) {
            return missingToken();
        }
        return archive.delete(ownerToken, id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    /** 잘못된 id·토큰 형식은 400 으로 돌려준다(스택트레이스 노출 방지). */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> onBadInput(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private static ResponseEntity<?> missingToken() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", OWNER_HEADER + " 헤더가 필요합니다"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
