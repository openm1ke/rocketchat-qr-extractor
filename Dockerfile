# Используем официальный образ OpenJDK как базовый
FROM openjdk:17-jdk-slim

# Устанавливаем рабочий каталог
WORKDIR /app

# Создайте директорию для логов
RUN mkdir -p /app/logs

# Копируем JAR файл из локальной системы в контейнер
COPY target/rocketchat-0.0.1-SNAPSHOT.jar /app/rocketchat.jar

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "rocketchat.jar"]
