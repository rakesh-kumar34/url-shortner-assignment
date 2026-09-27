FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY config config
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system shortline && useradd --system --gid shortline --home-dir /app shortline
WORKDIR /app
COPY --from=build --chown=shortline:shortline /build/target/shortline-1.0.0.jar app.jar
USER shortline
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
