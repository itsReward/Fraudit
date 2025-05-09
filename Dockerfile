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

# Build with Gradle
RUN ./gradlew bootJar --info

# Check if the JAR exists
RUN ls -la build/libs/

# Add the run script and make it executable
COPY run.sh /app/
RUN chmod +x /app/run.sh

# Expose port 8080
EXPOSE 8080

# Use the run script as the entrypoint
ENTRYPOINT ["/app/run.sh"]
