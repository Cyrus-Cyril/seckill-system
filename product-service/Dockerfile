FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY common-core/pom.xml common-core/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY common-core/src common-core/src
COPY product-service/src product-service/src
RUN mvn -pl product-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/product-service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
