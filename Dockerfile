# Etapa de construcción
FROM maven:3.9.6-amazoncorretto-17-al2023 AS build
WORKDIR /project
COPY ./ /project
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM amazoncorretto:17.0.10-al2023
WORKDIR /app

# Copiar el .jar compilado desde la etapa anterior
COPY --from=build /project/target/*.jar app.jar

# Usar el perfil de producción
ENV SPRING_PROFILES_ACTIVE=prod

# Comando para ejecutar la app
CMD ["java", "-jar", "app.jar"]
