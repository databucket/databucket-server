ARG DATABUCKET_JAR_PATH
FROM amazoncorretto:17

USER 1000
WORKDIR /app

COPY ./build/libs/databucket-server-*.jar app.jar
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
