# Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS build

WORKDIR /app

# Copy the entire system folder content
COPY system/ .

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage  
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
