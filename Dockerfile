# ---- Build stage: compiles the API and bundles the web client into a single jar
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY api/ api/
COPY client/ client/
WORKDIR /workspace/api
RUN sh ./mvnw -B -q -DskipTests package

# ---- Runtime stage: small JRE image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api/target/pizza-api.jar pizza-api.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "pizza-api.jar"]
