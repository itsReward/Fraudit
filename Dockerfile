# Build Java application
FROM gradle:7.6.1-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./
COPY src ./src
RUN gradle bootJar --no-daemon

# Create final image with Node.js and Java
FROM node:16-slim
WORKDIR /app

# Install OpenJDK
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Copy Node.js files
COPY server.js package.json ./
RUN npm install

# Copy Java app from build stage
COPY --from=build /app/build/libs/*.jar ./app.jar

# Create uploads directory
RUN mkdir -p /app/uploads

# Expose port
EXPOSE 8080

# Start Node.js server
CMD ["node", "server.js"]