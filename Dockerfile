FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy gradle files first for better layer caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Run gradle wrapper to download dependencies
RUN ./gradlew --version

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew build --no-daemon

# Command to run the application
CMD ["java", "-jar", "build/libs/server-1.0-SNAPSHOT.jar"]