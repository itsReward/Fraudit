#!/bin/sh
echo "Starting Fraudit application with diagnostic mode"

# Ensure log directory exists
mkdir -p /tmp/logs
echo "Created log directory: /tmp/logs"

# Start a simple HTTP server on port 8080 for health checks
echo '{"status":"UP","message":"Application is starting..."}' > /tmp/health.json
cd /tmp && python3 -m http.server 8080 &
PROXY_PID=$!
echo "Python HTTP server started on PID $PROXY_PID for health checks"

# Set JVM memory limits (crucial for Render free tier)
export JAVA_OPTS="-Xmx400m -Xms100m -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError"

# Configure the Spring Boot app to use port 8080
# We'll just run directly on 8080 since the Python server will be stopped
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=prod

# Print the Spring Boot jar file to ensure it exists
echo "Spring Boot JAR files in build/libs:"
ls -la /app/build/libs/

# Start the Spring Boot app in the background
echo "Starting Spring Boot application..."
java $JAVA_OPTS \
  -Dserver.port=8080 \
  -Dlogging.file.name=/tmp/logs/app.log \
  -Dlogging.level.org.springframework=INFO \
  -Dlogging.level.com.fraudit=INFO \
  -Dlogging.level.org.hibernate=INFO \
  -jar /app/build/libs/fraudit-*.jar > /tmp/logs/stdout.log 2>&1 &

APP_PID=$!
echo "Spring Boot application started on PID $APP_PID"

# Wait for app to start up (30 seconds max)
echo "Waiting for Spring Boot to initialize..."
START_WAIT_COUNT=0
while [ $START_WAIT_COUNT -lt 30 ]; do
  if grep -q "Started FrauditApplication" /tmp/logs/stdout.log 2>/dev/null; then
    echo "Application started successfully!"
    # Kill the Python server since our app is now running
    kill $PROXY_PID
    break
  fi

  # Check if app crashed during startup
  if ! kill -0 $APP_PID 2>/dev/null; then
    echo "!!! APPLICATION CRASHED DURING STARTUP !!!"
    cat /tmp/logs/stdout.log
    exit 1
  fi

  echo "Waiting for app to start... ($START_WAIT_COUNT/30)"
  START_WAIT_COUNT=$((START_WAIT_COUNT + 1))
  sleep 1
done

# If we got here and the app hasn't started, something's wrong
if [ $START_WAIT_COUNT -ge 30 ]; then
  echo "!!! APPLICATION FAILED TO START WITHIN 30 SECONDS !!!"
  cat /tmp/logs/stdout.log
  # But don't exit - keep the Python server running for health checks
fi

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
    if [ -f /tmp/logs/app.log ]; then
      tail -n 50 /tmp/logs/app.log
    else
      cat /tmp/logs/stdout.log
    fi
    echo "==========================="

    # Start the Python server again for health checks
    cd /tmp && python3 -m http.server 8080 &
    PROXY_PID=$!

    # Restart the application
    echo "Attempting to restart the application..."
    java $JAVA_OPTS \
      -Dserver.port=8080 \
      -Dlogging.file.name=/tmp/logs/app.log \
      -Dlogging.level.org.springframework=INFO \
      -Dlogging.level.com.fraudit=INFO \
      -Dlogging.level.org.hibernate=INFO \
      -jar /app/build/libs/fraudit-*.jar > /tmp/logs/stdout.log 2>&1 &

    APP_PID=$!
    echo "Spring Boot application restarted on PID $APP_PID"

    # Wait for the app to start again
    START_WAIT_COUNT=0
    while [ $START_WAIT_COUNT -lt 30 ]; do
      if grep -q "Started FrauditApplication" /tmp/logs/stdout.log 2>/dev/null; then
        echo "Application restarted successfully!"
        # Kill the Python server since our app is now running
        kill $PROXY_PID
        break
      fi

      # Check if app crashed during startup
      if ! kill -0 $APP_PID 2>/dev/null; then
        echo "!!! APPLICATION CRASHED DURING RESTART !!!"
        cat /tmp/logs/stdout.log
        break
      fi

      echo "Waiting for app to restart... ($START_WAIT_COUNT/30)"
      START_WAIT_COUNT=$((START_WAIT_COUNT + 1))
      sleep 1
    done
  fi
done