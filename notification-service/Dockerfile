# Build from Back-End-NDuy: docker build -f notification-service/Dockerfile -t mychatapp:notification .
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

RUN apk add --no-cache bash

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY common common
COPY notification-service notification-service

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -f common/pom.xml clean install -DskipTests \
    && ./mvnw -f notification-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 10001 app && adduser -u 10001 -G app -D app

COPY --from=build /build/notification-service/target/*-SNAPSHOT.jar /app/app.jar

ENV NOTIFICATION_SERVICE_PORT=8084

EXPOSE 8084

USER 10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
