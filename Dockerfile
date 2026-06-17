FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/minicache.jar .
RUN mkdir -p /data
ENV SAVE_DIR=/data
EXPOSE 6380
CMD ["java", "-jar", "minicache.jar", "server"]