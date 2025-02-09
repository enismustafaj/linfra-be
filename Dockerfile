FROM amazoncorretto:21

ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseSerialGC"
COPY target/resource-app*.jar resource-app.jar
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar resource-app.jar --server.port=${PORT}"]
