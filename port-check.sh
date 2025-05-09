#!/bin/sh
# This file helps verify port binding on container startup

echo "Starting port availability check..."
sleep 60  # Wait for application to start

# Check if port 8080 is listening
if netstat -tuln | grep LISTEN | grep -q :8080; then
    echo "SUCCESS: Port 8080 is open and listening"
else
    echo "ERROR: Port 8080 is not listening!"
    netstat -tuln
fi

# Keep the script running
tail -f /dev/null