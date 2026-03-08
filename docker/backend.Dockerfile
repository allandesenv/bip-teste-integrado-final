FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY ejb-module/pom.xml ejb-module/pom.xml
COPY backend-module/pom.xml backend-module/pom.xml
RUN mvn -B -q -pl backend-module -am dependency:go-offline

COPY ejb-module/src ejb-module/src
COPY backend-module/src backend-module/src
RUN mvn -B -pl backend-module -am -DskipTests clean package

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

COPY --from=build /workspace/backend-module/target/backend-module-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
