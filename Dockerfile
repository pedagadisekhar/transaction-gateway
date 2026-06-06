# Multi-stage build for Spring Boot application
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN ./mvnw dependency:resolve -DskipTests

COPY src src

RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-Xms512m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
