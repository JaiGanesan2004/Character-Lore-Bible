# A lightweight Java Runtime
FROM eclipse-temurin:21-jdk-alpine-3.21

#Setting the working directory
WORKDIR /app

#Copying the jar file into the container
COPY build/libs/*.jar app.jar

#Copying the uploads dir and env file
COPY .env .env
RUN mkdir -p uploads

#Exposing port for application to run
EXPOSE 8080

#Command to run the app
CMD ["java", "-jar", "app.jar"]