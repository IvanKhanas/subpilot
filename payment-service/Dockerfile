FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache gcompat
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.gradle,sharing=locked \
    ./gradlew :payment-service:bootJar -x test -x ktlintCheck --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/payment-service/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
