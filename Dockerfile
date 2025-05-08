FROM gradle:7.6.1-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src

# Run the Gradle build
RUN gradle bootJar --no-daemon

FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the JAR file with its original name
COPY --from=build /app/build/libs/*.jar ./app.jar

# Create a directory for uploads
RUN mkdir -p /app/uploads

# Explicitly expose port 8080
EXPOSE 8080

# Very explicit Java command with server properties
CMD ["java", "-Dserver.port=8080", "-Dserver.address=0.0.0.0", "-jar", "app.jar"]