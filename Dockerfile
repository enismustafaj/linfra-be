FROM openjdk:21-slim

COPY target/resource-app*.jar resource-app.jar

ENTRYPOINT ["java -Xms256m -Xmx512m -XX:+UseSerialGC -XX:MaxRAMPercentage=80 -jar resource-app.jar --server.port=${PORT}"]
