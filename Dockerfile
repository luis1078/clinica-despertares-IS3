# ---------- Etapa 1: build ----------
# Compila el proyecto con Maven Wrapper dentro del propio contenedor,
# así nadie necesita tener JDK 21 ni Maven instalados en su máquina.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Se copian primero los archivos de dependencias para aprovechar la cache de
# capas de Docker: si solo cambia el código fuente, este paso no se repite.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B clean package -DskipTests

# ---------- Etapa 2: runtime ----------
# Imagen final liviana: solo el JRE y el .jar ya compilado (sin Maven ni código fuente).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/*.jar app.jar
USER spring

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -q --spider http://localhost:8080/login || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
