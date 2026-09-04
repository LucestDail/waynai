package com.waynai.demo.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.waynai.demo.dto.SavedPlanDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 계획 파일 저장소.
 *
 * <p>DB 를 두지 않고 JSON 파일로 보관한다 — 이 워크스페이스의 다른 서비스와 같은 방식이고,
 * 계획 건수가 개인 단위(소유자당 수십 건)라 인덱스나 쿼리가 필요 없기 때문이다.
 * 나중에 규모가 커지면 이 클래스만 JPA 구현으로 바꾸면 된다.
 *
 * <p>배치: {@code <root>/<ownerHash>/<planId>.json}. 소유자별 디렉토리로 나눠
 * 목록 조회가 전체 스캔이 되지 않게 한다.
 */
@Slf4j
@Repository
public class PlanStore {

    /** id·ownerHash 로 허용할 문자. 경로 조작(../)을 원천 차단한다. */
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9_-]{1,128}$");

    /** 소유자당 보관 상한. 넘으면 오래된 것부터 지운다(프론트 localStorage 상한과 같은 값). */
    private static final int MAX_PER_OWNER = 50;

    private final Path root;
    private final ObjectMapper mapper;

    public PlanStore(@Value("${waynai.storage.dir:./data/plans}") String dir,
                     ObjectMapper mapper) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.mapper = mapper;
        try {
            Files.createDirectories(root);
            log.info("[plan-store] 저장 경로: {}", root);
        } catch (IOException e) {
            log.error("[plan-store] 저장 경로 생성 실패: {}", root, e);
        }
    }

    /** 저장(또는 덮어쓰기). 상한을 넘으면 가장 오래된 것부터 정리한다. */
    public void save(SavedPlanDto plan) throws IOException {
        requireSafe(plan.getOwnerHash(), "ownerHash");
        requireSafe(plan.getId(), "id");

        Path dir = root.resolve(plan.getOwnerHash());
        Files.createDirectories(dir);

        // 원자적 쓰기: 임시 파일에 쓴 뒤 옮긴다. 도중에 죽어도 반쪽 JSON 이 남지 않는다.
        Path target = dir.resolve(plan.getId() + ".json");
        Path tmp = dir.resolve(plan.getId() + ".json.tmp");
        mapper.writeValue(tmp.toFile(), plan);
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        pruneIfNeeded(dir);
    }

    /** 소유자의 계획 목록(최신순). 본문이 커서 목록에는 {@code plan} 을 비워 반환한다. */
    public List<SavedPlanDto> listByOwner(String ownerHash) {
        requireSafe(ownerHash, "ownerHash");
        Path dir = root.resolve(ownerHash);
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<SavedPlanDto> out = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
            for (Path p : ds) {
                readFile(p).ifPresent(sp -> {
                    sp.setPlan(null);      // 목록에는 본문을 싣지 않는다
                    sp.setFlights(null);
                    out.add(sp);
                });
            }
        } catch (IOException e) {
            log.warn("[plan-store] 목록 조회 실패: {}", dir, e);
        }
        out.sort(Comparator.comparing(SavedPlanDto::getSavedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return out;
    }

    /** 소유자의 계획 1건(본문 포함). */
    public Optional<SavedPlanDto> find(String ownerHash, String id) {
        requireSafe(ownerHash, "ownerHash");
        requireSafe(id, "id");
        return readFile(root.resolve(ownerHash).resolve(id + ".json"));
    }

    /**
     * 소유자를 모르는 상태에서 id 로만 찾는다(공유 링크용).
     *
     * <p>전체 디렉토리를 훑으므로 목록 조회보다 비싸다. id 는 UUID 라 추측이 사실상 불가능하고,
     * 공유는 "링크를 아는 사람만 본다"는 전제다.
     */
    public Optional<SavedPlanDto> findByIdAcrossOwners(String id) {
        requireSafe(id, "id");
        try (DirectoryStream<Path> owners = Files.newDirectoryStream(root)) {
            for (Path ownerDir : owners) {
                if (!Files.isDirectory(ownerDir)) continue;
                Path candidate = ownerDir.resolve(id + ".json");
                if (Files.isRegularFile(candidate)) {
                    return readFile(candidate);
                }
            }
        } catch (IOException e) {
            log.warn("[plan-store] 공유 조회 실패: {}", id, e);
        }
        return Optional.empty();
    }

    /** 삭제. 지운 게 있으면 true. */
    public boolean delete(String ownerHash, String id) {
        requireSafe(ownerHash, "ownerHash");
        requireSafe(id, "id");
        try {
            return Files.deleteIfExists(root.resolve(ownerHash).resolve(id + ".json"));
        } catch (IOException e) {
            log.warn("[plan-store] 삭제 실패: {}/{}", ownerHash, id, e);
            return false;
        }
    }

    private Optional<SavedPlanDto> readFile(Path p) {
        if (!Files.isRegularFile(p)) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(p.toFile(), SavedPlanDto.class));
        } catch (IOException e) {
            log.warn("[plan-store] 파일 읽기 실패(건너뜀): {}", p, e);
            return Optional.empty();
        }
    }

    /** 상한 초과분을 오래된 것부터 제거. */
    private void pruneIfNeeded(Path dir) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.json")) {
            List<Path> files = new ArrayList<>();
            ds.forEach(files::add);
            if (files.size() <= MAX_PER_OWNER) {
                return;
            }
            files.sort(Comparator.comparingLong(f -> f.toFile().lastModified()));
            for (int i = 0; i < files.size() - MAX_PER_OWNER; i++) {
                Files.deleteIfExists(files.get(i));
            }
            log.info("[plan-store] 상한 초과 {}건 정리", files.size() - MAX_PER_OWNER);
        } catch (IOException e) {
            log.warn("[plan-store] 정리 실패: {}", dir, e);
        }
    }

    private static void requireSafe(String s, String what) {
        if (s == null || !SAFE.matcher(s).matches()) {
            throw new IllegalArgumentException("허용되지 않는 " + what + ": " + s);
        }
    }
}
