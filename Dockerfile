FROM amazoncorretto:21

COPY target/resource-app*.jar resource-app.jar

ENTRYPOINT ["java -Xms256m -Xmx512m -XX:+UseSerialGC -jar resource-app.jar --server.port=${PORT}"]
