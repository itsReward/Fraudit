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
COPY --from=build /app/build/libs/fraudit.jar ./
EXPOSE 8080

# Create a directory for uploads
RUN mkdir -p /app/uploads

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080

ENTRYPOINT ["java", "-jar", "fraudit.jar"]