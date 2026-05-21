# IEUM 구현 기록

## 2026-05-19
- `application.properties`가 `templates` 아래에 있어 Spring Boot가 기본 설정으로 읽지 못했다.
- 테스트는 별도 `test` 프로필과 내장 데이터베이스가 없으면 컨텍스트 로딩부터 실패한다.
- 기존 코드에는 검색, 추천, 채팅, 알림의 일부 기반이 이미 있으므로 새로 갈아엎지 않고 빈 연결부를 채우는 방식이 안전하다.
- CSRF를 다시 켜면 기존 회원가입 POST 폼에도 토큰이 필요하다.
- 매칭 완료 시 지원 상태를 `COMPLETED`로 바꾸면 후기 작성 로직은 `ACCEPTED`뿐 아니라 `COMPLETED` 지원도 확정 매칭으로 인정해야 한다.
- 회원가입 단계의 복합 DTO는 `@RequestParam`이 아니라 `@ModelAttribute`로 받아야 폼 필드가 정상 바인딩된다.
- `@MapsId` 공유 PK 엔티티는 부모 엔티티만 연결하고 직접 ID를 세팅하지 않는 편이 안전하다. 수동 ID 세팅은 Hibernate의 null identifier 오류로 이어질 수 있다.
- 회원가입 완료처럼 상태를 변경하는 작업은 GET 완료 페이지가 아니라 마지막 POST 요청에서 처리해야 새로고침과 중복 호출 위험을 줄일 수 있다.
## 2026-05-20 ex6 회원 기능 보정
- Controller가 `/member/list`, `/member/read`, `/member/modify`를 반환해도 Service 구현과 실제 `templates/member/*` 파일이 없으면 화면 흐름은 완성되지 않는다.
- 회원 삭제처럼 다른 테이블이 회원을 참조하는 경우에는 같은 트랜잭션에서 참조 데이터(리뷰)를 먼저 삭제한 뒤 회원을 삭제해야 FK 오류를 피할 수 있다.
