# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy POMs first so the dependency layer caches independently of source changes.
COPY pom.xml .
COPY inventory-service/pom.xml inventory-service/
COPY reservation-service/pom.xml reservation-service/
RUN mvn -B -pl reservation-service -am dependency:go-offline

COPY reservation-service/src reservation-service/src
RUN mvn -B -pl reservation-service -am package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV TZ=UTC
COPY --from=build /build/reservation-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]