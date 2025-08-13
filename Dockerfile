# 빌드 스테이지
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle 설정 복사
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# 의존성 캐시
RUN ./gradlew dependencies --no-daemon || true

# 전체 소스 복사
COPY . .

# 빌드
RUN ./gradlew bootJar --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21-jre

WORKDIR /app

# jar 자동 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]