FROM eclipse-temurin:17
LABEL maintainer="junsong"
COPY sentinel-dashboard.jar /app/sentinel-dashboard.jar
EXPOSE 8718
ENTRYPOINT ["java", "-jar", "/app/sentinel-dashboard.jar", "--server.port=8718"]
