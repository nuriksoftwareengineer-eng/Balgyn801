# syntax=docker/dockerfile:1

# ─── Stage 1: Build ──────────────────────────────────────────────────────────
# Official Gradle image — Gradle 9.4.1 pre-installed, no distribution zip download.
# Verify tag: https://hub.docker.com/_/gradle/tags?name=9.4.1-jdk21
FROM gradle:9.4.1-jdk21 AS builder

WORKDIR /home/gradle/src

# Dependency descriptors first — Docker caches this layer independently from source.
# Layer is invalidated only when build.gradle or settings.gradle changes.
COPY --chown=gradle:gradle build.gradle settings.gradle ./

# Pre-warm Maven artifact cache before source is available.
# --mount=type=cache persists downloaded jars across builds, including --no-cache runs.
# uid/gid=1000 matches the 'gradle' user in the official image.
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle dependencies --no-daemon

# Copy source and build the executable fat JAR.
# Tests run in CI separately; Docker build skips them with -x test.
COPY --chown=gradle:gradle src ./src
RUN --mount=type=cache,target=/home/gradle/.gradle,uid=1000,gid=1000 \
    gradle bootJar --no-daemon -x test

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
# Amazon Corretto 21 — slim JRE, no Gradle or JDK overhead in the final image.
FROM amazoncorretto:21
RUN yum install -y curl && yum clean all
WORKDIR /app

COPY --from=builder /home/gradle/src/build/libs/app.jar dev-backend-spring.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "dev-backend-spring.jar"]
