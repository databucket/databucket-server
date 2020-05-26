FROM "openjdk:8"

COPY ./build/libs/databucket-app-2.2.6.jar /app.jar
COPY ./entrypoint.sh /entrypoint.sh
RUN chmod 755 /entrypoint.sh
ENTRYPOINT /entrypoint.sh


