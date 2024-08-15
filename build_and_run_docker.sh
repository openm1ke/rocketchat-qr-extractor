#!/bin/bash

# Задайте имя вашего Docker образа и контейнера
IMAGE_NAME="rocketchat-service"
CONTAINER_NAME="rocketchat_service"

echo "Сборка Docker образа..."
docker-compose build

echo "Остановка и удаление старого контейнера, если он существует..."
docker-compose down

echo "Запуск нового контейнера..."
docker-compose up -d

echo "Готово! Ваш контейнер работает. Проверьте логи с помощью 'docker-compose logs -f'."
