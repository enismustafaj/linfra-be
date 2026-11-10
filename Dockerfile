FROM eclipse-temurin:21-jre

COPY target/resource-app*.jar /app/resource-app.jar

# Shell form so ${PORT} expands; exec so java gets signals (SIGTERM for graceful shutdown).
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=80 -jar /app/resource-app.jar --server.port=${PORT:-8080}"]
