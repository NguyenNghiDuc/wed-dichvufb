FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /work
COPY pom.xml ./
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build --chown=10001:10001 /work/target/fbservice-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
STOPSIGNAL SIGTERM
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=65s --retries=3 CMD wget -q -O - http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]
