FROM amazoncorretto:17-alpine

WORKDIR /app

# Install debugging tools
RUN apk add --no-cache bash curl netcat-openbsd


# Add port check script
COPY port-check.sh ./
RUN chmod +x ./port-check.sh

# Copy run script first and make it executable
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

# Explicitly expose port 8080
EXPOSE 8080

# Use the run script for startup
ENTRYPOINT ["./run.sh"]
