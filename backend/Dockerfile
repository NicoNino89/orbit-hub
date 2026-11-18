# syntax=docker/dockerfile:1.7
# Imagem do back-end do Orbit Hub (KAN-52): build com Maven, execução em JRE 21 enxuta e sem root.

# ---------- build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/orbit-hub-api.jar extract --layers --launcher --destination /extraido

# ---------- execução ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S orbit && adduser -S -G orbit -H -s /sbin/nologin orbit \
 && mkdir -p /dados/arquivos && chown -R orbit:orbit /dados
WORKDIR /app
COPY --from=build /extraido/dependencies/ ./
COPY --from=build /extraido/spring-boot-loader/ ./
COPY --from=build /extraido/snapshot-dependencies/ ./
COPY --from=build /extraido/application/ ./
USER orbit:orbit
ENV ORBIT_ARMAZENAMENTO_PASTA=/dados/arquivos \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness >/dev/null || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
