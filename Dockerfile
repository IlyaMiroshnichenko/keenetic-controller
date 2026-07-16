# --- Этап 1: Сборка приложения внутри Docker ---
FROM gradle:8-jdk21-alpine AS builder
WORKDIR /app

# Копируем файлы конфигурации Gradle для кэширования зависимостей
COPY build.gradle settings.gradle ./
RUN gradle build --no-daemon > /dev/null 2>&1 || true

# Копируем исходный код и собираем чистый JAR
COPY src ./src
RUN gradle bootJar --no-daemon

# --- Этап 2: Запуск готового JAR ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем JAR-файл из этапа сборки
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar

# Указываем порт, который слушает Tomcat
EXPOSE 8080

# Команда запуска с принудительной активацией профиля prod
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
