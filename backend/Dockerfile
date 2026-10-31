# --- build ---
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q package -DskipTests

# --- runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
