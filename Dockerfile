FROM gradle:6.8.3-jdk15 AS builder
WORKDIR /home/money-tracker

COPY src src
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

RUN gradle clean assemble


FROM adoptopenjdk/openjdk15:jre-15.0.2_7-alpine

COPY --from=builder /home/money-tracker/build/libs/money-tracker-0.0.1-SNAPSHOT.jar /money-tracker/money-tracker.jar
VOLUME /money-tracker/database

WORKDIR /money-tracker
ENTRYPOINT java -jar money-tracker.jar