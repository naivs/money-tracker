FROM adoptopenjdk/openjdk15:jre-15.0.2_7-alpine

COPY build/libs/money-tracker-0.0.1-SNAPSHOT.jar /money-tracker/money-tracker.jar
VOLUME /money-tracker

WORKDIR /money-tracker
ENTRYPOINT java -jar money-tracker.jar