# syntax=docker/dockerfile:1

# ===== 빌드 스테이지 =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 레이어 캐싱: 빌드 스크립트 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 bootJar (tailwind는 스킵 — output.css는 레포에 포함됨)
COPY src src
RUN ./gradlew bootJar -x buildTailwind -x downloadTailwind --no-daemon

# ===== 런타임 스테이지 =====
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
