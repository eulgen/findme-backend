# FROM eclipse-temurin:21-jre-jammy

# # Create a non-privileged user to run the app
# ARG UID=10001
# RUN adduser \
#     --disabled-password \
#     --gecos "" \
#     --home "/nonexistent" \
#     --shell "/sbin/nologin" \
#     --no-create-home \
#     --uid "${UID}" \
#     appuser

# USER appuser
# WORKDIR /app

# # Copy the pre-built JAR file directly from target directory
# COPY target/*.jar app.jar

# EXPOSE 8080

# ENTRYPOINT ["java", "-jar", "app.jar"]

# Étape 1 : build
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src ./src
RUN ./mvnw -B package -DskipTests

# Étape 2 : exécution
# Étape 2 : exécution
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN addgroup --system app && adduser --system --ingroup app app && \
    mkdir -p /app/uploads && \
    chown -R app:app /app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
