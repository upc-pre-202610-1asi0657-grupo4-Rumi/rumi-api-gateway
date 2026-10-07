# Imagen de un microservicio Rumi. Copiar a la raíz del repositorio.
# El puerto se fija con --build-arg PORT=8081 (o con la variable SERVER_PORT en ejecución).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
ARG PORT=8080
ENV SERVER_PORT=${PORT}
WORKDIR /app
RUN useradd --system --no-create-home rumi
COPY --from=build /app/target/*.jar app.jar
USER rumi
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
