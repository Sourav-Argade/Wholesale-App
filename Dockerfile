# ========================================
# Dockerfile - Wholesale App (Spring Boot)
# ========================================
# Multi-stage build:
#   Stage 1 - Build the JAR using Maven
#   Stage 2 - Run the JAR in a lightweight JRE

# ---- Stage 1: Build ----
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy Maven Wrapper first for better layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Spring Boot health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
