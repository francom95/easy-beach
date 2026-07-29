# Etapa 20 - imagen de produccion del backend.
# Build multi-stage: compila con Maven+JDK completo, corre con solo un JRE
# (imagen final mucho mas chica, sin herramientas de build en produccion).

# ---------------------------------------------------------------- build
# El proyecto no tiene Maven Wrapper (mvnw) - se usa la imagen oficial de
# Maven en vez de asumir uno.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachear dependencias en su propia capa: solo se re-descargan si pom.xml cambia.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline || true

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# ---------------------------------------------------------------- runtime
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# No correr como root en el contenedor.
RUN groupadd --system easybeach && useradd --system --gid easybeach easybeach

COPY --from=build /app/target/easybeach-backend-*.jar /app/app.jar

# Los assets subidos (fotos de producto, branding) viven en un volumen aparte
# (ver docker-compose.yml) - este directorio es donde ASSET_STORAGE_ROOT_DIR
# los espera montados.
RUN mkdir -p /var/easybeach/assets && chown -R easybeach:easybeach /var/easybeach

USER easybeach
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
