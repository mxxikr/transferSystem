# 빌드 스테이지
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 권한 부여 및 설정 복사
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY build.gradle settings.gradle gradlew ./
COPY module-api/build.gradle module-api/build.gradle
COPY module-service/build.gradle module-service/build.gradle
COPY module-domain/build.gradle module-domain/build.gradle
COPY module-common/build.gradle module-common/build.gradle

# 의존성 캐시
RUN ./gradlew :module-api:dependencies --no-daemon || true

# 전체 소스 복사
COPY . .

# 빌드
RUN ./gradlew :module-api:bootJar --no-daemon

# 실행 스테이지
FROM eclipse-temurin:21-jre
WORKDIR /app

# jar 자동 복사
COPY --from=builder /app/module-api/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]