FROM amazoncorretto:21
COPY target/resource-app*.jar resource-app.jar
EXPOSE 8080

ENTRYPOINT ["java","-jar","/resource-app.jar"]
