FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY Back-End/service-discovery/ ./
COPY Back-End/appointment-service/mvnw ./mvnw
COPY Back-End/appointment-service/.mvn/ ./.mvn/
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /build/target/service-discovery-*.jar /app/app.jar
USER 10001
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
