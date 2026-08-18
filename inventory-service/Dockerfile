# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy POMs first so the dependency layer caches independently of source changes.
COPY pom.xml .
COPY inventory-service/pom.xml inventory-service/
COPY reservation-service/pom.xml reservation-service/
RUN mvn -B -pl inventory-service -am dependency:go-offline

COPY inventory-service/src inventory-service/src
RUN mvn -B -pl inventory-service -am package -DskipTests

# ---------- run ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV TZ=UTC
COPY --from=build /build/inventory-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]