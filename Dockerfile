FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline --batch-mode --no-transfer-progress -q

COPY src/ src/
RUN ./mvnw package --batch-mode --no-transfer-progress -DskipTests -Dspring.profiles.active=prod

FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S finassist && adduser -S finassist -G finassist

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

USER finassist

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]