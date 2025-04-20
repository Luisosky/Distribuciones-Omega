FROM openjdk:17-slim

WORKDIR /app

COPY target/distribuciones-omega.jar /app/distribuciones-omega.jar

EXPOSE 8080

CMD ["java", "-jar", "distribuciones-omega.jar"]