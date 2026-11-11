FROM eclipse-temurin:21-jre
WORKDIR /app
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
EXPOSE 3100
ENTRYPOINT ["java","-jar","/app/app.jar"]