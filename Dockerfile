FROM bellsoft/liberica-openjdk-alpine:17@0b748350bee68e69a8de8226208327e197f02e42332df469b8f5e5cb4aa5c03b
EXPOSE 8080:8080
COPY build/libs/*.jar app.jar
CMD ["dumb-init", "--"]
ENTRYPOINT ["java","-jar","app.jar"]
