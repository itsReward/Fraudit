#!/bin/sh
echo "Starting application on port: ${PORT}"
java -Dserver.port=${PORT} -jar /app/build/libs/fraudit.jar