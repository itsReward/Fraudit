#!/bin/bash

echo "Starting enhanced rebuild process for Fraudit..."

# Delete all build related directories
echo "Removing build directories..."
rm -rf build/
rm -rf .gradle/
rm -rf out/
rm -rf target/

# Delete Gradle cache for Flyway specifically
echo "Clearing Flyway from Gradle cache..."
rm -rf ~/.gradle/caches/modules-2/files-2.1/org.flywaydb

# Clean the Gradle cache for this project
echo "Cleaning Gradle cache for this project..."
./gradlew cleanBuildCache

# Refresh the Gradle dependencies
echo "Refreshing Gradle dependencies..."
./gradlew --refresh-dependencies

# Clean and build the project (skipping tests for speed)
echo "Building the project..."
./gradlew clean build -x test

echo "Enhanced rebuild completed."
echo "Running the application..."
./gradlew bootRun