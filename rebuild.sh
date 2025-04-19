#!/bin/bash

# Clean build directory
echo "Cleaning project..."
./gradlew clean

# Build the project
echo "Building project..."
./gradlew build -x test

# Run the application
echo "Starting application..."
./gradlew bootRun