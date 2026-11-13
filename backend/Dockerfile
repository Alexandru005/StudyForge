# ---------- build ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Dependintele se descarca intr-un layer separat (cache intre build-uri)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline || true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
ENV TZ=Europe/Bucharest
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
