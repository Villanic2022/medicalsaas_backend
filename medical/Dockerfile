# ETAPA 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Configurar encoding UTF-8
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

COPY . .
# Compilamos saltando tests para ir más rápido
RUN mvn clean package -DskipTests

# ETAPA 2: Ejecución (Run)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copiamos el JAR generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Configuración de memoria para el plan gratuito (CRÍTICO)
# Render Free tiene 512MB. Le damos 350MB a Java y dejamos el resto al sistema.
ENV JAVA_OPTS="-Xms256m -Xmx350m"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]