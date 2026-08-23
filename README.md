# WénLib — Interactive Novel Platform

Платформа для чтения, публикации интерактивных новелл и общения пользователей в реальном времени.

## Стек технологий

### Backend
- **Java 25** (с поддержкой Virtual Threads)
- **Spring Boot 4.0.5** (Spring Web, Spring Data JPA, Spring Security 7, Spring WebSocket)
- **PostgreSQL 18.3** (миграции Flyway)
- **Valkey 9.0.3 / Redis** (кэширование, сессии, лимитирование)
- **MinIO S3** (хранение обложек и аватаров)
- **Bucket4j 8.18.0** (Rate Limiting по алгоритму Token Bucket через Lettuce)
- **JJWT 0.13.0** (JWT Access + HttpOnly Refresh токены)
- **SpringDoc OpenAPI 2.8.13** (Swagger документация)
- **Apache Tika 3.2.3** (валидация медиа-файлов)

### Frontend
- **Vue 3.5** (Composition API, `<script setup>`)
- **TypeScript 5.9**
- **Vite 7.2**
- **Pinia 3.0** (стейт-менеджмент)
- **Vue Router 4.6**
- **Axios 1.13**
- **StompJS 7.3 & SockJS 1.6** (WebSockets)
- **FingerprintJS 5.2**
- **Vitest & Playwright** (Unit и E2E тесты)

---

## Архитектура и реализованный функционал

### 1. Безопасность и защита от ботов
- **Proof-of-Work (SHA-256):** динамический расчет челленджа с регулируемой сложностью в Valkey/Redis. Поддерживает инъекцию заголовков через Nginx `sub_filter` и работу напрямую через REST API.
- **Rate Limiting:** раздельные лимиты запросов на базе Bucket4j для анонимных гостей (`guest_id` с HMAC-подписью) и авторизованных пользователей.
- **Аутентификация:** Stateless JWT (Access) + HttpOnly SameSite Refresh-токены с ротацией и поддержкой отзыва сессий.

### 2. Контент и чтение
- **Интерактивный ридер:** выбор тем оформления, сохранение истории и прогресса чтения.
- **Редактор глав:** публикация, черновики, отложенная публикация по расписанию (`ScheduledChapterPublisher`).
- **Счетчик просмотров:** периодическая синхронизация просмотров из Redis в PostgreSQL (`ViewCountSyncScheduler`).

### 3. Социальные функции и Real-time
- Личные диалоги и групповые чаты через WebSockets (STOMP).
- Древовидные комментарии к главам новелл с реакциями.
- Система подписок, друзей и списков «Близкие друзья».
- Пользовательские библиотеки (статусы: *Читаю, В планах, Брошено, Прочитано*).

---

## Запуск проекта

### 1. Инфраструктура (Docker)
```bash
cd backend/docker
docker-compose up -d
```
Поднимает:
- PostgreSQL на порту `5432`
- MinIO на портах `9000` (API) и `9001` (Web Console)
- Valkey на порту `6379`

### 2. Backend
```bash
cd backend
cp .env.example .env
./mvnw.cmd spring-boot:run
```
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3. Frontend
```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```
- Dev-сервер: `http://localhost:5173`

---

## Структура репозитория

```text
├── backend/                  # Серверная часть на Spring Boot 4
│   ├── docker/               # docker-compose.yml (PostgreSQL 18.3, Valkey 9.0.3, MinIO)
│   ├── src/main/java/        # Исходный код (контроллеры, сервисы, security, сущности)
│   ├── src/main/resources/   # Конфигурация и Flyway SQL-миграции (31 миграция)
│   └── pom.xml
├── frontend/                 # Клиентская часть на Vue 3 + TypeScript
│   ├── src/                  # Компоненты, представления, хранилища Pinia, API
│   ├── e2e/                  # Playwright E2E тесты
│   └── package.json
└── README.md
```
