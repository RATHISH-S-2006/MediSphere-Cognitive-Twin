FROM eclipse-temurin:25-jdk AS build

# Download and install Maven 3.9.9 directly to avoid apt-get pulling old Java dependencies
RUN apt-get update && apt-get install -y wget tar && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz && \
    tar xzf apache-maven-3.9.9-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.9 /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn

WORKDIR /app
COPY pom.xml .
COPY src ./src

# Explicitly verify java and mvn versions as requested for Phase 1
RUN java -version
RUN mvn -version

# Compile and package the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
