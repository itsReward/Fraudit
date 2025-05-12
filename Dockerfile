FROM amazoncorretto:17-alpine

WORKDIR /app

# Install minimal necessary tools
RUN apk add --no-cache bash curl

# Create log directory
RUN mkdir -p /tmp/logs && chmod -R 777 /tmp/logs

# Copy run script
COPY run.sh ./
RUN chmod +x ./run.sh

# Copy Gradle files for better layer caching
COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Create upload directory
RUN mkdir -p /opt/render/project/uploads && chmod -R 777 /opt/render/project/uploads

# Build with Gradle
RUN ./gradlew bootJar --info

# Verify JAR exists
RUN ls -la build/libs/

# Explicitly expose port
EXPOSE 8080

# Start Spring Boot application directly
ENTRYPOINT ["./run.sh"]