#!/bin/sh
echo "Starting Fraudit application with diagnostic mode"

# Start port proxy on port 8080 for Render health checks
python3 -m http.server 8080 &
PROXY_PID=$!
echo "Python HTTP server started on PID $PROXY_PID for health checks"

# Set JVM memory limits (crucial for Render free tier)
export JAVA_OPTS="-Xmx400m -Xms100m -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError"

# Disable Flyway temporarily to debug application startup
export SPRING_FLYWAY_ENABLED=true

# Use a different port for the actual application to avoid conflict
export SERVER_PORT=8081
export SPRING_PROFILES_ACTIVE=prod

# Run the application with memory constraints and diagnostic options
java $JAVA_OPTS \
  -Dserver.port=8081 \
  -Dlogging.level.org.springframework=DEBUG \
  -Dlogging.level.com.fraudit=DEBUG \
  -Dlogging.level.org.hibernate=DEBUG \
  -jar /app/build/libs/fraudit.jar &

APP_PID=$!
echo "Spring Boot application started on PID $APP_PID with port 8081"

# Log system resources periodically
while true; do
  sleep 30
  echo "===== SYSTEM RESOURCES ====="
  free -m
  ps -o pid,rss,command | grep java
  echo "==========================="

  # Check if the app is still running
  if ! kill -0 $APP_PID 2>/dev/null; then
    echo "!!! APPLICATION CRASHED !!!"
    echo "Last few lines of logs:"
    tail -n 50 /tmp/app.log
    echo "==========================="
  fi
done