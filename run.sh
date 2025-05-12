#!/bin/sh
echo "Starting Fraudit application"

# Ensure log directory exists
mkdir -p /tmp/logs
echo "Created log directory: /tmp/logs"

# Set JVM memory limits (crucial for Render free tier)
export JAVA_OPTS="-Xmx400m -Xms100m -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError"

# Configure the Spring Boot app to use port 8080
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod

# Print the Spring Boot jar file to ensure it exists
echo "Spring Boot JAR files in build/libs:"
ls -la /app/build/libs/

# Get the exact JAR filename (without using wildcards)
JAR_FILE=$(ls -1 /app/build/libs/fraudit*.jar | head -n 1)
echo "Using JAR file: $JAR_FILE"

# Start the Spring Boot app in the foreground
echo "Starting Spring Boot application..."
exec java $JAVA_OPTS \
  -Dserver.port=8080 \
  -Dlogging.file.name=/tmp/logs/app.log \
  -Dlogging.level.org.springframework=INFO \
  -Dlogging.level.com.fraudit=INFO \
  -Dlogging.level.org.hibernate=INFO \
  -jar $JAR_FILE