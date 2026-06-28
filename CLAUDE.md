# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**이음(ieum)** — 장애인(USER)과 활동지원사(CAREGIVER)를 연결하는 매칭 플랫폼.  
Spring Boot 4 + Thymeleaf + MySQL 기반 서버사이드 렌더링 웹앱.

## 빌드 및 실행 명령어

```bash
# 앱 실행 (로컬 프로파일 기본)
./gradlew bootRun

# 빌드 (Tailwind CSS 자동 포함)
./gradlew build

# 테스트 전체 실행
./gradlew test

# 단일 테스트 클래스 실행
./gradlew test --tests "com.project.ieum.service.MatchingServiceTest"

# 단일 테스트 메서드 실행
./gradlew test --tests "com.project.ieum.service.MatchingServiceTest.메서드명"

# Tailwind CSS만 빌드
./gradlew buildTailwind

# Querydsl Q클래스 생성
./gradlew compileJava
```

## 환경 설정

- 기본 프로파일: `local` → `application-local.yml` 사용
- DB: MySQL (`db7` 스키마, 포트 3306), 환경변수 `IEUM_DB_URL` / `IEUM_DB_USERNAME` / `IEUM_DB_PASSWORD`로 오버라이드
- 메일: Gmail SMTP, 환경변수 `IEUM_MAIL_USERNAME` / `IEUM_MAIL_PASSWORD`
- 파일 업로드 경로: `IEUM_CERTIFICATION_UPLOAD_PATH` (기본 `~/ieum/uploads/certifications`)
- 매칭 스케줄러: `application.yml`에서 기본 off — 운영 시 `matching.scheduler.enabled=true`

## 아키텍처

### 레이어 구조

```
Controller → Service → Repository → Entity
```

- **Controller**: URL 라우팅 + 뷰 렌더링 / REST JSON 응답 혼용
  - Thymeleaf 뷰 반환: `CaregiverBoardController`, `DisabledBoardController`, `MatchingController` 등
  - REST API: `/api/**` 하위 컨트롤러 (`ChatRestController`, `NotificationController` 등)
  - STOMP: `ChatStompController`, `PresenceStompController`, `MarketStompController`
- **Service**: 비즈니스 로직 집중. `@Transactional` 기본, 조회 전용은 `readOnly = true`
- **Repository**: Spring Data JPA 기본 + Querydsl 복합 검색 (`CaregiverSearchRepositoryImpl`)

### 사용자 역할 (UserRole enum)

| 역할 | 설명 | 접근 경로 |
|---|---|---|
| `USER` | 장애인 (도움 요청자) | `/disabled/**` |
| `CAREGIVER` | 활동지원사 | `/caregiver/**` |
| `ADMIN` | 관리자 | `/admin/**` |

로그인 성공 시 역할에 따라 자동 리다이렉트 (`SecurityConfig.resolveRedirectUrl`).

### 핵심 도메인 흐름

1. **매칭**: 장애인이 `HelpRequest` 생성 → 활동지원사가 지원(`HelpRequestApplication`) → 매칭 확정 시 `Conversation` 생성
2. **추천**: `RecommendationService`가 성격 태그·가용시간·거리 기반 점수 계산 후 활동지원사 순위화
3. **채팅**: STOMP WebSocket (`/ws` 엔드포인트), 구독 경로 `/topic/conversation/{id}`
4. **이음마켓**: 활동지원사 간 물품 거래 게시판 + 마켓 전용 채팅 (`/market/**`)
5. **알림**: `NotificationService` → SSE 또는 DB 저장 후 폴링

### 지오코딩

`GeocodingService` 인터페이스 구현체: `KakaoGeocodingService` (카카오 주소 API).  
지도 마커 아이콘은 `static/images/kakaomap/` 하위에 위치.

### CSS

Tailwind CSS 독립형 CLI (`tailwindcss.exe`) 사용. Node.js 불필요.  
- 입력: `src/main/resources/static/css/input.css`
- 출력: `src/main/resources/static/css/output.css` (minify)
- `processResources` 태스크에 자동 연결되어 빌드 시 항상 실행됨

### 보안

- `SecurityConfig`: 역할별 URL 접근 제어, `/api/**`는 CSRF 비활성화
- XSS: Jsoup HTML 새니타이저 (`org.jsoup:jsoup:1.18.3`)
- 세션 쿠키: `HttpOnly` + `SameSite=Lax`

### 테스트

- 테스트 DB: H2 인메모리 + `H2VarcharEnumDialect` (enum → varchar 변환, MySQL enum 충돌 방지)
- 설정: `application-test.yml` / `application-test.properties`

### GlobalModelAdvice

`GlobalModelAdvice`가 모든 뷰에 공통 모델 속성 주입 (로그인 사용자 정보, 미읽음 알림 등).  
`SecurityModelAdvice`도 별도로 Security 관련 속성 주입.


### 언어 설정
항상 한국어로 응답할 것.