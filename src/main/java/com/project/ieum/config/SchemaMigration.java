package com.project.ieum.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigration implements ApplicationRunner {

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        dropUqHrRequesterDate();
    }

    // uq_hr_requester_date: 엔티티에서 제거된 unique 제약조건이지만
    // ddl-auto=update 는 기존 제약조건을 삭제하지 않아 DB에 잔존.
    // MySQL이 이 인덱스를 requester_id FK의 서포팅 인덱스로 사용 중이므로
    // 먼저 일반 인덱스를 추가한 뒤 unique 제약조건을 제거한다.
    private void dropUqHrRequesterDate() {
        try (Connection conn = dataSource.getConnection()) {
            // 제약조건이 아직 존재하는지 확인
            ResultSet rs = conn.getMetaData().getIndexInfo(
                    conn.getCatalog(), null, "help_requests", true, false);
            boolean exists = false;
            while (rs.next()) {
                if ("uq_hr_requester_date".equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    exists = true;
                    break;
                }
            }
            rs.close();

            if (!exists) {
                return; // 이미 제거됨
            }

            log.info("[SchemaMigration] uq_hr_requester_date 제약조건 제거 시작");
            // requester_id FK 서포팅 인덱스 추가 후 unique 제약조건 삭제
            conn.createStatement().execute(
                    "ALTER TABLE help_requests ADD INDEX idx_hr_requester_id (requester_id)");
            conn.createStatement().execute(
                    "ALTER TABLE help_requests DROP INDEX uq_hr_requester_date");
            log.info("[SchemaMigration] uq_hr_requester_date 제약조건 제거 완료");

        } catch (Exception e) {
            log.warn("[SchemaMigration] 제약조건 제거 실패 (이미 제거됐거나 무관한 오류): {}", e.getMessage());
        }
    }
}
