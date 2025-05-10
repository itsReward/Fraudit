#!/bin/sh
echo "Starting application with port configuration"

# Start the port proxy in the background
./port-proxy.sh &
PROXY_PID=$!

# Export necessary environment variables
export SERVER_PORT=8080
export SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
export SPRING_FLYWAY_LOCATIONS=classpath:db/migration
export SPRING_PROFILES_ACTIVE=prod

# Wait a moment for the proxy to bind
sleep 2

echo "Port proxy started on PID $PROXY_PID"
echo "Starting Spring Boot application..."

# Start the Spring Boot application
java -Xmx400m -Xms100m -jar /app/build/libs/fraudit.jar
java -Dserver.port=8080 -Dspring.main.web-application-type=servlet -jar /app/build/libs/fraudit.jar &
APP_PID=$!

# Wait for the application to complete startup
wait $APP_PID

# If we get here, the app has exited - kill the proxy
kill $PROXY_PID

# Exit with the same code as the app
exit $?