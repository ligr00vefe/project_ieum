# 이음 (ieum) — Care Match

**장애인과 활동지원사를 연결하는 성향 기반 케어 매칭 플랫폼**

이음은 도움이 필요한 장애인(케어메이트)과 활동지원사가 서로를 신뢰하며 만날 수 있도록 돕는 웹 서비스입니다. 단순 조건 매칭을 넘어 성격 태그·가용 시간·거리를 종합한 추천으로 잘 맞는 파트너를 찾아주고, 매칭 전 실시간 채팅으로 충분히 소통한 뒤 활동을 시작할 수 있습니다.

## 주요 기능

### 🤝 매칭
- 장애인이 도움 요청(HelpRequest)을 등록하면 활동지원사가 지원하고, 수락 시 매칭이 확정됩니다.
- 매칭 확정과 동시에 1:1 대화방이 열려 바로 소통할 수 있습니다.

### 🎯 성향 기반 추천
- 성격 태그(MBTI 포함), 가용 시간, 거리 기반 점수를 계산해 활동지원사를 순위화하여 추천합니다.

### 💬 실시간 채팅
- STOMP WebSocket 기반 1:1 채팅으로 매칭 전후 소통을 지원합니다.
- 접속 상태(프레즌스) 표시를 함께 제공합니다.

### 📅 일정 달력
- 등록/지원 중, 매칭 확정, 활동 완료 일정을 마이페이지 미니 달력과 상세 달력에서 확인할 수 있습니다.
- 상태를 색상과 도형(빈 원/채운 원)으로 이중 구분해 색약 사용자도 쉽게 식별할 수 있습니다.

### 🛒 이음마켓
- 성장하거나 장애 유형이 바뀌어 쓰지 않게 된 보조기구·복지 용품을 활동지원사 간 거래하는 중고 마켓입니다.
- 마켓 전용 실시간 채팅과 거래 후기(매너온도)를 제공합니다.

### 🔔 알림
- 매칭·채팅·거래 이벤트를 SSE 실시간 알림 또는 DB 폴링으로 전달합니다.

### ♿ 접근성
- **글자 크게 보기**: 전체 UI를 150%로 확대하는 토글을 제공합니다.
- **다크모드**: 라이트/다크/시스템 테마를 지원하며, 달력 마커 등 색상이 테마에 맞게 자동 보정됩니다.

### 🗺️ 위치 기반 서비스
- TMap API로 주소를 좌표로 변환(지오코딩)하고 장소를 검색해 거리 기반 매칭에 활용합니다.

## 사용자 역할

| 역할 | 설명 | 주요 경로 |
|---|---|---|
| `USER` | 장애인 (도움 요청자, 케어메이트) | `/disabled/**` |
| `CAREGIVER` | 활동지원사 | `/caregiver/**` |
| `ADMIN` | 관리자 | `/admin/**` |

로그인하면 역할에 따라 각자의 게시판으로 자동 이동합니다.

## 기술 스택

| 구분 | 기술 |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Security, Spring Data JPA, Querydsl |
| Frontend | Thymeleaf (SSR), Tailwind CSS (독립형 CLI, Node.js 불필요), Vanilla JS |
| Database | MySQL 8 (운영/로컬), H2 (테스트) |
| 실시간 | STOMP WebSocket (채팅), SSE (알림) |
| 외부 연동 | TMap 지오코딩/장소 검색, Gmail SMTP (메일 발송) |
| 보안 | Jsoup HTML 새니타이저 (XSS 방어), HttpOnly + SameSite=Lax 세션 쿠키 |

## 시작하기

### 요구 사항
- JDK 17
- MySQL 8 (스키마 `db7`, 포트 3306)

### 실행

```bash
# 앱 실행 (기본 프로파일: local)
./gradlew bootRun

# 빌드 (Tailwind CSS 자동 포함)
./gradlew build

# 테스트
./gradlew test
```

첫 실행 시 `DataInitializer`가 로컬 테스트 계정을 자동 생성합니다.

### 환경 변수

| 변수 | 설명 | 기본값 |
|---|---|---|
| `IEUM_DB_URL` | MySQL 접속 URL | `jdbc:mysql://localhost:3306/db7` |
| `IEUM_DB_USERNAME` / `IEUM_DB_PASSWORD` | DB 계정 | — |
| `IEUM_MAIL_USERNAME` / `IEUM_MAIL_PASSWORD` | Gmail SMTP 계정 | — |
| `IEUM_CERTIFICATION_UPLOAD_PATH` | 자격증 파일 업로드 경로 | `~/ieum/uploads/certifications` |
| `TMAP_APP_KEY` (`tmap.app-key`) | TMap API 키 | — |

매칭 스케줄러는 기본 비활성화되어 있으며, 운영 환경에서 `matching.scheduler.enabled=true`로 켭니다.

### Tailwind CSS

Node.js 없이 독립형 CLI(`tailwindcss.exe`)를 사용합니다. 빌드 시 자동으로 실행되지만 수동 빌드도 가능합니다.

```bash
./gradlew buildTailwind
```

- 입력: `src/main/resources/static/css/input.css`
- 출력: `src/main/resources/static/css/output.css` (minify)

## 아키텍처

```
Controller → Service → Repository → Entity
```

- **Controller**: Thymeleaf 뷰 렌더링과 REST JSON(`/api/**`)을 혼용. 채팅·프레즌스·마켓은 STOMP 컨트롤러 별도 운영
- **Service**: 비즈니스 로직 집중. `@Transactional` 기본, 조회 전용은 `readOnly = true`
- **Repository**: Spring Data JPA + Querydsl 복합 검색

### 핵심 도메인 흐름

1. 장애인이 `HelpRequest` 생성 → 활동지원사가 지원 → 매칭 확정 시 `Conversation` 생성
2. `RecommendationService`가 성격 태그·가용시간·거리 기반으로 활동지원사 순위화
3. 활동 완료 후 상호 리뷰 작성

### 테스트

- H2 인메모리 DB + 커스텀 `H2VarcharEnumDialect` (MySQL enum 충돌 방지)
- 단일 테스트: `./gradlew test --tests "com.project.ieum.service.MatchingServiceTest"`

## 프로젝트 구조

```
src/main/java/com/project/ieum/
├── config/          # Security, WebSocket, 데이터 초기화 등 설정
├── controller/      # 뷰 + REST + STOMP 컨트롤러
├── service/         # 비즈니스 로직 (매칭, 추천, 채팅, 마켓, 알림 …)
├── repository/      # JPA + Querydsl
├── entity/          # 도메인 엔티티
└── dto/             # 요청/응답 DTO

src/main/resources/
├── templates/       # Thymeleaf 뷰 (disabled, caregiver, market, chat …)
└── static/          # CSS(Tailwind), JS, 이미지
```
