# syntax=docker/dockerfile:1
#
# Сборка JAR внутри образа — не нужен локальный `./gradlew bootJar` перед docker build.
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test

FROM amazoncorretto:21
RUN yum install -y curl && yum clean all
WORKDIR /app

COPY --from=builder /app/build/libs/app.jar dev-backend-spring.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dev-backend-spring.jar"]
