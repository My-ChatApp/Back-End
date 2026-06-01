# Build from Back-End-NDuy: docker build -f chat-service/Dockerfile -t mychatapp:chat .
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

RUN apk add --no-cache bash

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY common common
COPY chat-service chat-service

RUN sed -i 's/\r$//' mvnw \
    && chmod +x mvnw \
    && ./mvnw -f common/pom.xml clean install -DskipTests \
    && ./mvnw -f chat-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 10001 app && adduser -u 10001 -G app -D app

COPY --from=build /build/chat-service/target/*-SNAPSHOT.jar /app/app.jar

ENV CHAT_SERVICE_PORT=8082

EXPOSE 8082

USER 10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
