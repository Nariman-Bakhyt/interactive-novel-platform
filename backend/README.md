# 📚 WénLib — Interactive Novel & Social Platform (Backend)

[![Java](https://img.shields.io/badge/Java-25-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0+-brightgreen.svg?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-7+-green.svg?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16+-blue.svg?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-Valkey_9.0-red.svg?logo=redis&logoColor=white)](https://redis.io/)
[![MinIO](https://img.shields.io/badge/MinIO-S3_Storage-C72C48.svg?logo=minio&logoColor=white)](https://min.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg?logo=docker&logoColor=white)](https://www.docker.com/)

Высоконагруженный бэкенд платформы для публикации, интерактивного чтения веб-новелл и социального взаимодействия пользователей в реальном времени.

---

## 🎯 Архитектурные особенности и ключевые решения

- **Java 25 & Virtual Threads (Project Loom):** Построен на передовой версии Java 25 с включенными виртуальными потоками (`spring.threads.virtual.enabled=true`) для максимально эффективной обработки асинхронных и блокирующих I/O операций.
- **Интеллектуальная защита от ботов (Proof-of-Work):** Собственный алгоритм генерации криптографических задач (SHA-256) с динамической сложностью в зависимости от активности IP/устройства в Redis. Поддерживает работу как через Nginx/OpenResty (`sub_filter` + `auth_request`), так и напрямую через REST API.
- **Многоуровневый Rate Limiting:** Реализован на базе алгоритма Token Bucket ([Bucket4j](https://github.com/bucket4j/bucket4j) + Lettuce Redis) с дифференцированными лимитами:
  - Строгие ограничения для анонимных гостей (`guest:<id>`).
  - Повышенные лимиты для авторизованных пользователей (`user:<id>`).
  - Защита эндпоинтов авторизации, регистрации и поиска от спама.
- **Двухуровневая безопасность (Spring Security 6):**
  - Stateless JWT-аутентификация для Access-токенов.
  - HttpOnly SameSite Refresh-токены в куках с HMAC-подписью и ротацией сессий.
  - Отзыв сессий (session revocation) и детекция смены устройств.
- **Хранение медиа (MinIO S3):** Изолированное хранение аватаров и обложек новелл в S3-совместимом хранилище с валидацией MIME-типов и сжатием.
- **Real-time коммуникация:** Полнодуплексный обмен сообщениями через WebSockets (STOMP) для личных чатов, диалогов и системы живых уведомлений.
- **Фоновые задачи и планировщик:**
  - `ScheduledChapterPublisher` — отложенная публикация глав по расписанию.
  - `ViewCountSyncScheduler` — периодическая асинхронная синхронизация счетчиков просмотров из Redis в PostgreSQL для снижения нагрузки на БД.
- **База данных и миграции:** Реляционная структура на PostgreSQL с версионированием через Flyway (индексы по внешним ключам, полнотекстовый поиск, мягкое удаление soft-delete, древовидные комментарии).

---

## 🛠️ Стек технологий

| Категория | Технологии |
|---|---|
| **Язык и платформа** | Java 25, Spring Boot 4.0, Spring Data JPA, Spring Web, Spring WebSocket |
| **Безопасность** | Spring Security 7, JJWT, HMAC-SHA256, BCrypt + Pepper |
| **Базы данных и кэш** | PostgreSQL 16, Redis 7 / Valkey 9, Hibernate, Flyway |
| **Rate Limiting** | Bucket4j, Lettuce Cache Manager |
| **Файловое хранилище** | MinIO S3 SDK |
| **Инфраструктура** | Docker, Docker Compose, Maven Wrapper |

---

## 🚀 Быстрый старт

### 1. Требования
- JDK 25+
- Docker и Docker Compose

### 2. Клонирование и настройка окружения
```bash
git clone https://github.com/Nariman-Bakhyt/interactive-novel-platform.git
cd interactive-novel-platform
cp .env.example .env
```

### 3. Запуск инфраструктуры (PostgreSQL, Redis, MinIO)
```bash
cd docker
docker-compose up -d
cd ..
```

### 4. Запуск бэкенда
```bash
# Linux / macOS:
./mvnw spring-boot:run

# Windows:
./mvnw.cmd spring-boot:run
```

Сервер запустится на `http://localhost:8080`.

---

## 📖 Документация API

После запуска Swagger UI доступен по адресу:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## 🏗️ Структура проекта

```text
InteractiveNovelPlatform/
├── src/main/java/project/interactivenovelplatform/
│   ├── config/          # Конфигурации Security, Redis, Bucket4j, S3, CORS, WebSocket
│   ├── controller/      # REST-контроллеры (Auth, Novel, User, Chat, Rating, etc.)
│   ├── dto/             # Request / Response DTO модели
│   ├── entity/          # JPA сущности (User, Novel, Chapter, Comment, Chat, etc.)
│   ├── repository/      # Spring Data JPA репозитории и Specifications
│   ├── security/        # JWT провайдер, ChallengeService (PoW), UserPrincipal
│   ├── service/         # Бизнес-логика и интерфейсы сервисов
│   │   ├── impl/        # Реализации сервисов
│   │   └── scheduler/   # Фоновые задачи (Sync просмотров, публикация глав)
│   └── error/           # Глобальная обработка исключений (GlobalExceptionHandler)
├── src/main/resources/
│   ├── db/migration/    # Версионированные SQL миграции Flyway (V1..V2_28)
│   └── application.properties
├── docker/              # Docker Compose (PostgreSQL, MinIO, Valkey)
└── pom.xml
```
