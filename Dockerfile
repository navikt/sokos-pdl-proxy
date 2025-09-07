FROM cgr.dev/chainguard/jdk:latest

COPY build/libs/*.jar app.jar

ENV TZ="Europe/Oslo"

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]