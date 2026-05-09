FROM amazoncorretto:21
COPY build/libs/dev-0.0.1-SNAPSHOT.jar dev-backend-spring.jar
ENTRYPOINT ["java", "-jar", "dev-backend-spring.jar"]