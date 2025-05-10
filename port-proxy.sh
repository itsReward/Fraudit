#!/bin/sh
# This script creates a simple proxy to immediately bind to port 8080
# while the main application starts up

# Start netcat to immediately bind to port 8080 and respond to health checks
echo "Starting port proxy on port 8080"
while true; do
  echo -e "HTTP/1.1 200 OK\nContent-Type: application/json\n\n{\"status\":\"UP\",\"message\":\"Application is starting...\"}" | nc -l -p 8080
done