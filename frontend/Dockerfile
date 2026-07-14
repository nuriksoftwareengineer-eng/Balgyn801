# syntax=docker/dockerfile:1
#
# Production frontend: React build → nginx static serving.
# VITE_API_BASE_URL defaults to /api/v1 (relative) so nginx can proxy to the backend.

FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --prefer-offline

COPY . .

ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Intro splash screen (holds a full-screen overlay ~3s on the first page of a session).
# Default true preserves existing behavior; production disables it via compose build args.
ARG VITE_ENABLE_SPLASH=true
ENV VITE_ENABLE_SPLASH=$VITE_ENABLE_SPLASH

RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
