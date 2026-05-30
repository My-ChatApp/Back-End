# Build from Back-End-NDuy: docker build -f user-service/Dockerfile -t mychatapp:user .
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

RUN apk add --no-cache bash

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY common common
COPY user-service user-service

RUN chmod +x mvnw \
    && ./mvnw -f common/pom.xml clean install -DskipTests \
    && ./mvnw -f user-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 10001 app && adduser -u 10001 -G app -D app

COPY --from=build /build/user-service/target/*-SNAPSHOT.jar /app/app.jar

ENV USER_SERVICE_PORT=8083

EXPOSE 8083

USER 10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
