package com.project.ieum.testsupport;

import org.hibernate.dialect.H2Dialect;

/**
 * 테스트 전용 H2 방언 — enum 컬럼을 native ENUM 대신 VARCHAR로 선언한다.
 *
 * <p>배경: Hibernate 7 {@link H2Dialect}는 {@code @Enumerated(STRING)} 필드를 H2의
 * native {@code ENUM('A','B')} 컬럼으로 생성한다. H2 2.4.240에서 이 native ENUM 컬럼이
 * 있는 테이블에 prepared statement로 INSERT하면, 같은 행의 다른 CHECK 제약 평가가
 * 오염되어({@code Check constraint invalid}) INSERT가 거부되는 마찰이 있다.
 *
 * <p>엔티티는 운영 MySQL 기준({@code columnDefinition="TEXT"} 등)이라 손대지 않고,
 * 테스트 슬라이스에서만 enum 선언을 표준 VARCHAR로 바꿔 이 문제를 제거한다.
 * (의미는 {@code @Enumerated(STRING)}과 동일 — 값 제약은 Hibernate가 별도 CHECK로 생성.)
 */
public class H2VarcharEnumDialect extends H2Dialect {

    @Override
    public String getEnumTypeDeclaration(String name, String[] values) {
        int max = 16;
        if (values != null) {
            for (String v : values) {
                if (v != null) {
                    max = Math.max(max, v.length());
                }
            }
        }
        return "varchar(" + max + ")";
    }

    // CHECK 제약 생성을 끈다. Hibernate 7은 JOINED 상속의 discriminator 값 제약을
    // CHECK로 자동 생성하는데, 그 CHECK가 H2 2.4.240의 prepared statement INSERT에서
    // "Check constraint invalid"로 거부되는 마찰이 있다. 슬라이스 테스트는 #9 겹침 쿼리
    // 의미 검증이 목적이고 DB 무결성 제약 검증이 아니므로, 테스트에서만 CHECK를 끈다.
    @Override
    public boolean supportsColumnCheck() {
        return false;
    }

    @Override
    public boolean supportsTableCheck() {
        return false;
    }
}
