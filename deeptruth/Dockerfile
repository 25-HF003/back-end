FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean build

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]