FROM maven:3-eclipse-temurin-24-alpine AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/coupon-service-1.0.0.jar /app/application.jar

USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/application.jar"]
