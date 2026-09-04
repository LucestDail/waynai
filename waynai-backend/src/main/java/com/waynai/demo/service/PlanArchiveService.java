package com.waynai.demo.service;

import com.waynai.demo.dto.FlightOfferDto;
import com.waynai.demo.dto.SavedPlanDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.repository.PlanStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 계획 보관 서비스 — 저장·목록·조회·삭제.
 *
 * <p>소유자 식별은 클라이언트가 만든 익명 토큰 하나로 한다. 서버는 그 토큰을 저장하지 않고
 * SHA-256 해시만 쓴다. 즉 저장 파일이 유출돼도 토큰을 되돌릴 수 없고, 서버는 애초에
 * 사용자가 누구인지 모른다(이메일·이름·비밀번호를 받지 않는다).
 *
 * <p>이 방식을 고른 이유:
 * <ul>
 *   <li>개인정보를 수집하지 않으므로 처리방침·동의·파기 절차 부담이 없다</li>
 *   <li>가입 절차 없이 바로 쓰이므로 이탈이 없다</li>
 *   <li>나중에 계정을 붙일 때 {@code ownerHash} 를 사용자 id 로 매핑하면 그대로 승격된다</li>
 * </ul>
 * 대가로 <b>토큰을 잃으면 계획도 잃는다</b>(복구 수단이 없다). 그래서 클라이언트는 토큰을
 * 사용자에게 보여주고 백업을 안내해야 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanArchiveService {

    private final PlanStore store;

    /** 저장하고 발급된 id 를 돌려준다. */
    public SavedPlanDto save(String ownerToken, TravelPlanDto plan, List<FlightOfferDto> flights) throws IOException {
        SavedPlanDto entry = SavedPlanDto.builder()
                .id(UUID.randomUUID().toString())
                .ownerHash(hash(ownerToken))
                .title(titleOf(plan))
                .savedAt(System.currentTimeMillis())
                .plan(plan)
                .flights(flights)
                .build();
        store.save(entry);
        log.info("[plan-archive] 저장: id={} title={}", entry.getId(), entry.getTitle());
        return entry;
    }

    /** 내 계획 목록(최신순, 본문 없음). */
    public List<SavedPlanDto> list(String ownerToken) {
        return store.listByOwner(hash(ownerToken));
    }

    /** 내 계획 1건(본문 포함). */
    public Optional<SavedPlanDto> get(String ownerToken, String id) {
        return store.find(hash(ownerToken), id);
    }

    /**
     * 공유 조회 — 토큰 없이 id 만으로 읽는다.
     *
     * <p>id 가 UUID 라 추측이 사실상 불가능한 것에 의존한다("링크를 아는 사람만 본다").
     * 민감정보를 담는 용도가 아니므로 이 정도가 적절하다고 판단했다.
     */
    public Optional<SavedPlanDto> getShared(String id) {
        return store.findByIdAcrossOwners(id);
    }

    public boolean delete(String ownerToken, String id) {
        return store.delete(hash(ownerToken), id);
    }

    private static String titleOf(TravelPlanDto plan) {
        if (plan == null) {
            return "여행 계획";
        }
        if (plan.getDestination() != null && !plan.getDestination().isBlank()) {
            return plan.getDestination();
        }
        if (plan.getTheme() != null && !plan.getTheme().isBlank()) {
            return plan.getTheme();
        }
        return "여행 계획";
    }

    /**
     * 소유자 토큰 → 저장용 해시.
     *
     * <p>package-private(테스트용). 같은 토큰은 항상 같은 해시가 나와야 하고,
     * 해시에서 토큰을 되돌릴 수 없어야 한다.
     */
    static String hash(String ownerToken) {
        if (ownerToken == null || ownerToken.isBlank()) {
            throw new IllegalArgumentException("소유자 토큰이 없습니다");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(ownerToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 을 쓸 수 없습니다", e);
        }
    }
}
