# IEUM 구현 TODO

## 2026-06-14 실시간 채팅 보강
- [x] 기존 ChatService/Repository/Entity/REST API는 유지하고 room.html 중심으로 STOMP 연결 및 REST fallback broadcast 추가
- [x] WebSocket 연결 성공 시 polling 중단, 실패 시 기존 REST 전송과 polling fallback 유지
- [x] 메시지 id 기반 중복 렌더링 방지와 읽음 처리 흐름 확인

## 기준
- HTML 24개 이슈 전체를 구현 범위로 본다.
- PDF 철학인 성향 기반 매칭, 첫 만남 안전, 당사자 중심 케어를 화면과 도메인 흐름에 반영한다.
- 설정 안정화와 테스트 가능 상태를 기능 구현보다 먼저 맞춘다.

## 진행 순서
- [ ] 프로필 기반 설정 정리 및 테스트 데이터소스 구성
- [ ] 공통 Thymeleaf 레이아웃, 로그인/로그아웃, CSRF 복구
- [ ] 도움 요청 작성/목록/상세/내 요청 흐름 구현
- [ ] 지원 즉시 채팅방 생성, 수락 시 매칭 확정, 잔여 지원 종료
- [ ] 채팅, 읽음 처리, 알림 뱃지 연결
- [ ] 활동 상태 전이, 후기 작성, 평점 집계
- [ ] 안전 가이드, 신고/차단, 자격증 증빙, 관리자 화면 구현
- [ ] 접근성 체크와 회귀 테스트
## 2026-05-20 ex6 회원 기능 보정
- [x] ex6 기본틀의 회원 Controller -> Service -> Repository -> Entity/DTO -> 화면 흐름 확인
- [x] 회원 목록/상세/수정/삭제 동작에 필요한 최소 파일만 수정
- [x] Gradle 빌드로 컴파일 확인

## 2026-06-08 실행 오류 점검
- [x] Gradle 테스트와 bootRun으로 실행 상태 확인
- [x] 시작 로그의 잘못된 접속 URL 수정
- [x] 수정 후 테스트 및 임시 실행 확인
## 2026-06-08 main 최종 반영
- [x] origin/main 최신 상태 확인
- [x] origin/kimjiwon/dev 최신 커밋을 main에 병합
- [x] /login 중복 매핑과 Querydsl 테스트 설정 보완
- [x] 회원가입 마지막 POST 단계에서 UserService 가입 처리로 정리
