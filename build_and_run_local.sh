#!/bin/bash

echo "Сборка приложения с помощью Maven..."
mvn clean package

# Задайте имя вашего JAR файла
JAR_NAME="rocketchat-0.0.1-SNAPSHOT.jar"

echo "Запуск приложения локально..."
java -jar target/$JAR_NAME

echo "Готово! Ваше приложение работает. Логи выводятся в консоль."
