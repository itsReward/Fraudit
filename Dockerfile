FROM amazoncorretto:17-alpine

WORKDIR /app

# Install debugging tools
RUN apk add --no-cache bash

# Copy Gradle files first for better layer caching
COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle.kts settings.gradle.kts ./

# Ensure gradlew is executable
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Create upload directory
RUN mkdir -p /opt/render/project/uploads && chmod -R 777 /opt/render/project/uploads

# Build with Gradle - using the specific JAR name from build.gradle.kts
RUN ./gradlew bootJar --info

# Check if the JAR exists
RUN ls -la build/libs/

# Use the specific JAR file name we set in build.gradle.kts
ENTRYPOINT ["java", "-jar", "/app/build/libs/app.jar"]