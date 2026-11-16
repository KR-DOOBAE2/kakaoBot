FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon :bot-app:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/bot-app/build/libs/*.jar app.jar
ENV PORT=10000
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "app.jar"]
