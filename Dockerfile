# Imagen de despliegue (Render no tiene runtime nativo de Java). Etapa 1 compila con Maven; etapa 2
# corre el jar con un JRE mínimo. Tags fijados por digest: Dependabot (ecosistema docker) los
# actualiza. La calidad (Spotless, Checkstyle, JaCoCo, pruebas) la verifica el CI, no esta imagen.
FROM maven:3.9-eclipse-temurin-25@sha256:93b8a14ea2f412782e4e842651273b4d903e35cc496284f178fbbe2d67d00976 AS build
WORKDIR /build
COPY .mvn .mvn
COPY pom.xml ./
COPY src src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61
RUN addgroup -S app && adduser -S -G app app
USER app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/quipu-app-sprmono-*.jar app.jar

# Valor por defecto para Render gratis (512 MB); sobrescribible desde la variable del servicio.
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# El puerto lo fija la variable PORT (application-dev.yml y application-prod.yml).
ENTRYPOINT ["java", "-jar", "app.jar"]
