package com.project.ieum.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// BasicEntity(@CreatedDate/@LastModifiedDate)의 감사 컬럼 자동 채움을 활성화한다.
// 별도 @Configuration으로 분리해 @DataJpaTest 슬라이스에서 @Import(JpaAuditingConfig.class)로 선택 로딩 가능.
// ⚠ 전 팀 공유 영향: 활성화 시 모든 BasicEntity 하위 엔티티의 created_at/updated_at이 채워지기 시작한다.
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
