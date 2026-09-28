FROM eclipse-temurin:25-jdk AS build
ARG SERVICE
WORKDIR /workspace
COPY server/ server/
COPY contracts/ contracts/
WORKDIR /workspace/server
RUN case "$SERVICE" in api-gateway|identity|users|interactions|activities) ;; *) exit 1 ;; esac \
    && chmod +x gradlew \
    && ./gradlew ":${SERVICE}:bootJar" --no-daemon \
    && cp "${SERVICE}/build/libs/"*.jar /tmp/application.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 bloom \
    && useradd --uid 10001 --gid bloom --no-create-home bloom
COPY --from=build --chown=bloom:bloom /tmp/application.jar /app/application.jar
USER bloom
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/application.jar"]
