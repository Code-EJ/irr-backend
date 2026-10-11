FROM maven:3.9.14-eclipse-temurin-21 AS build
# Bound build memory/parallelism on developer hosts; overridable without changing Maven goals.
# Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
ARG MAVEN_OPTS="-Xmx1024m -XX:ActiveProcessorCount=2"
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,id=s/efda17ad-6783-409e-ba81-73a30c459906-/root/.m2,target=/root/.m2 mvn --batch-mode -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
RUN apt-get update && apt-get upgrade -y --no-install-recommends \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 irr && useradd --uid 10001 --gid irr --create-home irr \
    && mkdir -p /app /data/uploads && chown -R irr:irr /app /data
WORKDIR /app
COPY --from=build --chown=irr:irr /build/target/api-0.0.1-SNAPSHOT.jar /app/irr.jar
COPY --chmod=0555 docker/backend-entrypoint.sh /app/backend-entrypoint.sh
USER 10001:10001
EXPOSE 9191
ENTRYPOINT ["/app/backend-entrypoint.sh"]
CMD ["java", "-jar", "/app/irr.jar"]
