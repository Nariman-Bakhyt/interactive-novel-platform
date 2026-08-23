# 📖 WénLib — Interactive Novel Platform (Frontend)

[![Vue.js](https://img.shields.io/badge/Vue.js-3.5+-4FC08D.svg?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7+-3178C6.svg?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-6.0+-646CFF.svg?logo=vite&logoColor=white)](https://vite.dev/)
[![Pinia](https://img.shields.io/badge/Pinia-2.3+-FFE873.svg?logo=pinia&logoColor=black)](https://pinia.vuejs.org/)
[![Axios](https://img.shields.io/badge/Axios-1.7+-5A29E4.svg?logo=axios&logoColor=white)](https://axios-http.com/)

Клиентская часть веб-платформы **WénLib** для чтения, создания интерактивных новелл, комментирования и общения в реальном времени.

---

## ✨ Основные модули и функционал

- **Интерактивный ридер новелл:**
  - Настраиваемый режим чтения (темы, шрифты, размеры текста, ночной режим).
  - Поблочный рендеринг контента и навигация по главам.
  - Сохранение прогресса и истории чтения (`reading_history`).
- **Редактор глав и панель автора:**
  - Создание и редактирование новелл, загрузка обложек.
  - Управление статусами публикации (черновик, опубликовано, отложенная публикация).
- **Социальный граф и интерактив:**
  - Система дружбы, подписчиков, заблокированных пользователей и списков «Близкие друзья».
  - Древовидные комментарии к главам с реакциями и цитированием.
  - Закладки и статусы прочтения в библиотеке (*Читаю, В планах, Брошено, Прочитано*).
- **Real-time мессенджер:**
  - Личные диалоги и групповые чаты через WebSockets (STOMP).
  - Живые уведомления о новых сообщениях и системных событиях.
- **Клиент защиты от ботов (Proof-of-Work):**
  - Асинхронное вычисление SHA-256 челленджа чанками без блокировки UI-потока браузера.
  - Автоматическая повторная верификация и обработка гостевых сессий.
- **Glassmorphic UI / Design System:**
  - Современный адаптивный интерфейс с полупрозрачными панелями, анимациями переходов и темной темой.

---

## 🛠️ Стек технологий

- **Фреймворк:** Vue 3 (Composition API, `<script setup>`)
- **Язык:** TypeScript
- **Сборщик:** Vite
- **Стейт-менеджмент:** Pinia
- **Маршрутизация:** Vue Router
- **Сетевой уровень:** Axios (интерцепторы авторизации, авто-рефреш JWT токенов, Fingerprinting)
- **Real-time:** StompJS, SockJS WebSockets
- **Тестирование и качество:** Playwright (E2E), Vitest, ESLint, Prettier

---

## 🚀 Быстрый старт

### 1. Установка зависимостей
```bash
npm install
```

### 2. Настройка окружения
```bash
cp .env.example .env
```

### 3. Запуск в режиме разработки
```bash
npm run dev
```
Приложение откроется на `http://localhost:5173`.

### 4. Сборка для продакшена
```bash
npm run build-only
```

---

## 🧪 Тестирование

```bash
# Запуск Unit тестов (Vitest)
npm run test:unit

# Запуск End-to-End тестов (Playwright)
npm run test:e2e
```
