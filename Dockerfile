# Build multi-stage: compila o boot jar e roda em uma imagem JRE enxuta.
# Java 21 (mesma toolchain do build.gradle.kts).

# --- Stage 1: build ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro os arquivos de build para aproveitar o cache de dependencias.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Copia o codigo-fonte e gera apenas o boot jar (sem testes).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuario nao-root para reduzir superficie de ataque.
RUN addgroup --system app && adduser --system --ingroup app app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
