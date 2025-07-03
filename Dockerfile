# --- Stage 1: Build the application ---
# Use a Maven and JDK 17 image as the builder
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml file to leverage Docker's layer caching
# This step will only be re-run if pom.xml changes
COPY pom.xml .

# Download all dependencies into the local Maven repository
# This creates a separate layer that is cached
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application, creating the JAR file. Skip tests for faster builds.
RUN mvn clean install -DskipTests


# --- Stage 2: Create the final, lightweight runtime image ---
# Use a minimal JRE image for a smaller footprint
# FROM eclipse-temurin:17-jre-alpine
FROM openjdk:17-jdk-slim

# Set the working directory
WORKDIR /app

# Create a non-root user and group for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Copy only the built JAR file from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set ownership of the application file to the non-root user
RUN chown appuser:appgroup app.jar

# Switch to the non-root user
USER appuser

# Expose the port the application runs on
EXPOSE 8080

# Add a health check to ensure the application is running correctly
# This requires the spring-boot-starter-actuator dependency
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD curl --fail http://localhost:8080/actuator/health || exit 1

# The command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]