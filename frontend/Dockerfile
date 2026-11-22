# syntax=docker/dockerfile:1.7
# Imagem do front-end do Orbit Hub (KAN-52): build com Vite, servido por nginx sem root.
# A mesma imagem roda no Docker Compose e na Azure; só muda ORBIT_API_UPSTREAM (para onde vai /api).

FROM node:24-alpine AS build
WORKDIR /src
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
ENV ORBIT_API_UPSTREAM=http://api:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1:8080/ >/dev/null || exit 1
