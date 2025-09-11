FROM openjdk:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy JAR file
COPY target/*.jar app.jar
COPY application-docker.properties ./config/application-docker.properties
COPY .env .env

# Create non-root user (делаем это ДО переключения пользователя)
RUN addgroup -S spring && adduser -S spring -G spring

# Создаем директорию для логов и даем права пользователю spring
RUN mkdir -p /app/logs && chown spring:spring /app/logs

# Переключаемся на non-root пользователя
USER spring

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]