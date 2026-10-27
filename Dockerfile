# 배포 파일은 ECR 이미지로 전달하고 SSM 명령 본문에는 포함하지 않는다.
FROM scratch AS deployment-files
COPY docker-compose-prod.yaml /opt/smap/deploy/docker-compose-prod.yaml
COPY db/init/ /opt/smap/deploy/db/init/

FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew && ./gradlew clean bootJar -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=deployment-files /opt/smap/deploy/ /opt/smap/deploy/
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-Xms256m", \
  "-Xmx384m", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
