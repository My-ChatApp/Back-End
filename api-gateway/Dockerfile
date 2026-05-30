# Build from Back-End-NDuy: docker build -f api-gateway/Dockerfile -t mychatapp:gateway .
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /build

RUN apk add --no-cache bash

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY common common
COPY api-gateway api-gateway

RUN chmod +x mvnw \
    && ./mvnw -f common/pom.xml clean install -DskipTests \
    && ./mvnw -f api-gateway/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -g 10001 app && adduser -u 10001 -G app -D app

COPY --from=build /build/api-gateway/target/*-SNAPSHOT.jar /app/app.jar

ENV SERVER_PORT=8080

EXPOSE 8080

USER 10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
