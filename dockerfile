FROM openjdk:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy JAR file
COPY target/*.jar app.jar
COPY .env .env
# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]