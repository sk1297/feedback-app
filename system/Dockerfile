FROM openjdk:17-jre-slim

# Install Maven
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Copy project files
COPY . .

# Build the application
RUN mvn clean package -DskipTests

# Expose port
EXPOSE 8080


# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Run the application
CMD ["java", "-jar", "target/*.jar"]