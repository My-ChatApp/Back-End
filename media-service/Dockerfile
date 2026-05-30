# Build from Back-End-NDuy: docker build -f media-service/Dockerfile -t mychatapp:media .
FROM node:22-alpine AS build

WORKDIR /app

COPY media-service/package.json media-service/package-lock.json ./
RUN npm ci

COPY media-service/tsconfig.json media-service/media-upload.config.json ./
COPY media-service/src ./src

RUN npm run build

FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production \
    MEDIA_SERVICE_PORT=8085

RUN addgroup -g 10001 app && adduser -u 10001 -G app -D app

COPY media-service/package.json media-service/package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=build /app/dist ./dist
COPY media-service/media-upload.config.json ./

EXPOSE 8085

USER 10001

CMD ["node", "dist/server.js"]
