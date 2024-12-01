FROM openjdk
COPY target/resource-app*.jar resource-app.jar
EXPOSE 8080

ENTRYPOINT ["java","-jar","/resource-app.jar"]
