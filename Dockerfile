FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /work
COPY pom.xml ./
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /work/target/fbservice-0.0.1-SNAPSHOT.jar /app/app.jar
COPY scripts/deployment-entrypoint.sh /app/deployment-entrypoint.sh
RUN chmod 0555 /app/deployment-entrypoint.sh
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s CMD wget -q -O - "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1
ENTRYPOINT ["/app/deployment-entrypoint.sh"]
