package com.waynai.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.SavedPlanDto;
import com.waynai.demo.dto.TravelPlanDto;
import com.waynai.demo.repository.PlanStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 계획 보관 계약 검증.
 *
 * <p>가장 중요한 두 가지는 <b>소유자 격리</b>(남의 계획이 보이면 안 된다)와
 * <b>경로 조작 방어</b>(id 로 파일시스템을 벗어나면 안 된다)다.
 */
class PlanArchiveServiceTest {

    @TempDir
    Path tmp;

    private PlanArchiveService archive;
    private PlanStore store;

    private static final String ALICE = "alice-token-abc123";
    private static final String BOB = "bob-token-xyz789";

    @BeforeEach
    void setUp() {
        store = new PlanStore(tmp.toString(), new ObjectMapper());
        archive = new PlanArchiveService(store);
    }

    private static TravelPlanDto plan(String dest) {
        return TravelPlanDto.builder().destination(dest).days(3).duration("3일").build();
    }

    @Test
    @DisplayName("저장한 계획을 같은 토큰으로 다시 읽는다")
    void saveAndGet() throws IOException {
        SavedPlanDto saved = archive.save(ALICE, plan("부산"), null);

        Optional<SavedPlanDto> found = archive.get(ALICE, saved.getId());

        assertTrue(found.isPresent());
        assertEquals("부산", found.get().getPlan().getDestination());
        assertEquals("부산", found.get().getTitle(), "제목은 목적지에서 딴다");
    }

    @Test
    @DisplayName("🔒 다른 토큰으로는 남의 계획이 보이지 않는다")
    void ownerIsolation() throws IOException {
        SavedPlanDto alicePlan = archive.save(ALICE, plan("부산"), null);

        assertTrue(archive.get(BOB, alicePlan.getId()).isEmpty(), "Bob 이 Alice 계획을 읽으면 안 된다");
        assertTrue(archive.list(BOB).isEmpty(), "Bob 목록에 Alice 계획이 섞이면 안 된다");
        assertFalse(archive.delete(BOB, alicePlan.getId()), "Bob 이 Alice 계획을 지우면 안 된다");
        assertTrue(archive.get(ALICE, alicePlan.getId()).isPresent(), "Alice 것은 그대로 남아야 한다");
    }

    @Test
    @DisplayName("목록은 최신순이고 본문을 싣지 않는다")
    void listIsNewestFirstWithoutBody() throws IOException {
        archive.save(ALICE, plan("부산"), null);
        archive.save(ALICE, plan("제주"), null);
        archive.save(ALICE, plan("강릉"), null);

        List<SavedPlanDto> list = archive.list(ALICE);

        assertEquals(3, list.size());
        assertEquals("강릉", list.get(0).getTitle(), "가장 최근 것이 먼저");
        assertNull(list.get(0).getPlan(), "목록에 본문을 싣지 않는다(응답 크기)");
    }

    @Test
    @DisplayName("공유 조회는 토큰 없이 id 만으로 된다")
    void sharedLookupNeedsNoToken() throws IOException {
        SavedPlanDto saved = archive.save(ALICE, plan("여수"), null);

        Optional<SavedPlanDto> shared = archive.getShared(saved.getId());

        assertTrue(shared.isPresent());
        assertEquals("여수", shared.get().getPlan().getDestination());
    }

    @Test
    @DisplayName("삭제하면 조회도 목록도 사라진다")
    void deleteRemoves() throws IOException {
        SavedPlanDto saved = archive.save(ALICE, plan("통영"), null);

        assertTrue(archive.delete(ALICE, saved.getId()));
        assertTrue(archive.get(ALICE, saved.getId()).isEmpty());
        assertTrue(archive.list(ALICE).isEmpty());
        assertFalse(archive.delete(ALICE, saved.getId()), "두 번 지우면 false");
    }

    @Test
    @DisplayName("🔒 id 에 경로 조작을 넣으면 거부한다")
    void rejectsPathTraversal() {
        for (String evil : List.of("../escape", "../../etc/passwd", "a/b", "..", "with space", "")) {
            assertThrows(IllegalArgumentException.class,
                    () -> store.find(PlanArchiveService.hash(ALICE), evil),
                    "거부해야 하는 id: " + evil);
        }
    }

    @Test
    @DisplayName("같은 토큰은 같은 해시, 다른 토큰은 다른 해시이고 원문이 남지 않는다")
    void hashIsStableAndOneWay() {
        String h1 = PlanArchiveService.hash(ALICE);
        String h2 = PlanArchiveService.hash(ALICE);

        assertEquals(h1, h2, "같은 토큰은 항상 같은 해시");
        assertNotEquals(h1, PlanArchiveService.hash(BOB));
        assertFalse(h1.contains("alice"), "해시에 원문 흔적이 남으면 안 된다");
        assertEquals(64, h1.length(), "SHA-256 hex 는 64자");
        assertEquals(h1, PlanArchiveService.hash("  " + ALICE + "  "), "앞뒤 공백은 무시");
    }

    @Test
    @DisplayName("토큰이 없으면 거부한다")
    void rejectsMissingToken() {
        assertThrows(IllegalArgumentException.class, () -> PlanArchiveService.hash(null));
        assertThrows(IllegalArgumentException.class, () -> PlanArchiveService.hash("  "));
    }

    @Test
    @DisplayName("소유자당 보관 상한을 넘으면 오래된 것부터 정리된다")
    void prunesOverLimit() throws IOException {
        for (int i = 0; i < 55; i++) {
            archive.save(ALICE, plan("도시" + i), null);
        }

        assertEquals(50, archive.list(ALICE).size(), "상한 50건을 넘지 않아야 한다");
    }
}
