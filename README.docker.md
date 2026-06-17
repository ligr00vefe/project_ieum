# Docker 로컬 실행 (포트폴리오 / 데모)

prod 프로파일 애플리케이션과 MySQL을 named volume 기반 compose로 띄운다.

## 사전 준비
- Docker / Docker Compose
- `.env` 생성:
  ```sh
  cp .env.example .env
  ```
  `.env`의 비밀번호를 실제 값으로 수정한다. (`.env`는 gitignored)

## 실행
```sh
docker compose up -d --build
```
- `ieum-mysql`(MySQL 8.0) + `ieum-app`(Spring Boot) 기동
- 데이터: named volume `ieum_mysql_data` (컨테이너 삭제와 무관하게 보존)
- 앱: http://localhost:8080  (기본 `APP_PORT`)
- MySQL: 호스트 포트 `33306`(기본 `MYSQL_PORT`) → 컨테이너 3306

## 동작 메모
- `SPRING_PROFILES_ACTIVE=prod` — 매칭 시간 자동전이 스케줄러 ON
- DB 접속은 compose 환경변수(`IEUM_DB_*`)로 주입 — `application-secret.properties`는 이미지에 포함되지 않는다.
- 첫 기동(빈 볼륨)은 `DDL_AUTO=update`로 스키마 자동생성. 이후 데이터 보존 시 `.env`에서 `validate`로 바꾼다.
- 헬스 체크: http://localhost:8080/actuator/health (actuator)

## 정지 / 정리
```sh
docker compose down        # 컨테이너 정지·삭제 (볼륨 보존)
docker compose down -v     # 볼륨까지 삭제 (데이터 완전 삭제)
```

## 팀 MySQL 마이그레이션 (별도 운영 절차)
기존 팀 MySQL(bind mount, 포트 30306)을 이 compose(named volume)로 옮기는 절차는
별도 운영 작업이다: 백업(`full-20260617.sql`) 복원 → 33306 스테이징 검증 → 30306 전환.
