#!/bin/sh
# Immediately bind to port 8080
echo "Starting port binding on 8080"
nc -l -k -p 8080 -e 'echo -e "HTTP/1.1 200 OK\nContent-Type: text/plain\n\nService is starting" | cat -' &
NC_PID=$!

# Now start the application
echo "Starting application..."
java -Dserver.port=5001 -jar /app/build/libs/fraudit.jar &
APP_PID=$!

# Wait for application to exit
wait $APP_PID
kill $NC_PID