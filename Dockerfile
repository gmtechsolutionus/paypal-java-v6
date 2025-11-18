FROM maven:3.9.6-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:8-jre
ENV JAVA_OPTS="-Xms256m -Xmx512m"
WORKDIR /app
COPY --from=build /workspace/target/paypal-java-v6-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]

