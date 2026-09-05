FROM eclipse-temurin:25-jdk AS build

WORKDIR /app
COPY pom.xml .
COPY .mvn ./.mvn
COPY mvnw .
COPY src ./src

RUN chmod +x mvnw
RUN java -version
RUN ./mvnw --version

# Compile and package without resolving or compiling test sources in the runtime image.
RUN ./mvnw clean package -Dmaven.test.skip=true

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
