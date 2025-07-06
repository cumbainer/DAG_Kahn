FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY src/ src/
RUN javac $(find src -name "*.java") \
    -d out                             && \
    jar --create --file /out/app.jar \
        --main-class App  \
        -C out .

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /out/app.jar .
ENTRYPOINT ["java","-jar","app.jar"]
