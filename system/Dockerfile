FROM eclipse-temurin:17-jdk

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean

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
