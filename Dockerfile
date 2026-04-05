FROM container-registry.oracle.com/java/openjdk:21

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Xms1024m", "-Xmx1024m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-Dspring.profiles.active=prod", "-jar", "app.jar"]