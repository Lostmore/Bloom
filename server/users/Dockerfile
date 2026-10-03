FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY identity/build.gradle identity/build.gradle
COPY api-gateway/build.gradle api-gateway/build.gradle
COPY interactions/build.gradle interactions/build.gradle
COPY activities/build.gradle activities/build.gradle
COPY users users
RUN chmod +x gradlew && ./gradlew :users:bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 bloom \
    && useradd --uid 10001 --gid bloom --no-create-home bloom
COPY --from=build --chown=bloom:bloom /workspace/users/build/libs/users.jar /app/users.jar
USER bloom
EXPOSE 8082
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-XX:+UseStringDeduplication", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "/app/users.jar"]
