FROM maven:3.9.16-eclipse-temurin-25 AS builder
WORKDIR /
COPY /src /src
COPY pom.xml /
COPY checkstyle-suppressions.xml .
RUN mvn -f /pom.xml clean package

FROM eclipse-temurin:25
WORKDIR /
COPY /src /src
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

