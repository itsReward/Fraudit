#!/bin/sh
echo "Starting application on properly configured port"
export SERVER_PORT=8080
export SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
export SPRING_FLYWAY_LOCATIONS=classpath:db/migration

# Force the port to be 8080
exec java -Dserver.port=8080 -Dspring.main.web-application-type=servlet -jar /app/build/libs/fraudit.jar