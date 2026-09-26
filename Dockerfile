FROM maven:3.9.14-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 irr && useradd --uid 10001 --gid irr --create-home irr \
    && mkdir -p /app /data/uploads && chown -R irr:irr /app /data
WORKDIR /app
COPY --from=build --chown=irr:irr /build/target/api-0.0.1-SNAPSHOT.jar /app/irr.jar
USER 10001:10001
EXPOSE 9191
ENTRYPOINT ["java", "-jar", "/app/irr.jar"]
