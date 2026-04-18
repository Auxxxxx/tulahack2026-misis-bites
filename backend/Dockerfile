FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY api/build.gradle ./api/
COPY compute/build.gradle ./compute/
COPY gradle/wrapper/ ./gradle/wrapper/
COPY gradlew ./

RUN chmod +x gradlew
RUN ./gradlew dependencies

COPY . .

RUN ./gradlew bootJar -x test

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=builder /app/api/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
