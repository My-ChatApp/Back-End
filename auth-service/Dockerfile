# Build from Back-End-NDuy: docker build -f auth-service/Dockerfile -t mychatapp:auth .
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

RUN apk add --no-cache bash

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY common common
COPY auth-service auth-service

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -f common/pom.xml clean install -DskipTests \
    && ./mvnw -f auth-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -g 10001 app && adduser -u 10001 -G app -D app

COPY --from=build /build/auth-service/target/*-SNAPSHOT.jar /app/app.jar

ENV AUTH_SERVICE_PORT=8081

EXPOSE 8081

USER 10001

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:8081/api/auth/health" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
