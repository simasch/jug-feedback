FROM azul/zulu-openjdk-alpine:25

VOLUME /tmp

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
