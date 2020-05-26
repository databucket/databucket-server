#!/bin/sh

JAVA_ARGS="-XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=90.0 -XX:MinRAMPercentage=50.0 -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Djava.security.egd=file:/dev/./urandom -DLog4jContextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector "

java ${JAVA_ARGS} -jar /app.jar

