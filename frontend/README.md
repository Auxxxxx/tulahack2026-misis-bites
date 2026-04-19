# MISIS Bites Frontend

Мобильное Vue.js приложение для управления командами и подбора кандидатов.

## Структура проекта

```
frontend/
├── src/
│   ├── api/          # API клиент для взаимодействия с бэкендом
│   ├── router/       # Vue Router конфигурация
│   ├── styles/       # Глобальные стили
│   ├── views/        # Страницы приложения
│   ├── App.vue       # Корневой компонент
│   └── main.js       # Точка входа
├── index.html
├── package.json
└── vite.config.js
```

## Маршруты

- `/` — Список всех команд
- `/team/:id` — Детальная информация о команде
- `/team/:teamId/member/:memberId` — Информация об участнике команды
- `/team/:teamId/candidates` — Подбор кандидатов (фильтрация по ролям)
- `/team/:teamId/candidate/:candidateId` — Детальная информация о кандидате

## Установка и запуск

```bash
# Установка зависимостей
npm install

# Запуск в режиме разработки
npm run dev

# Сборка для продакшена
npm run build
```

## API

Приложение использует бэкенд API по адресу `http://72.56.35.92:8080`

Основные эндпоинты:
- `GET /teams` — список команд
- `GET /teams/:id/analytics` — аналитика команды
- `GET /teams/:id/members` — участники команды
- `GET /teams/:teamId/members/:memberId/analytics` — аналитика участника
- `GET /teams/:teamId/members/:memberId/recommendations` — рекомендации по участнику
- `GET /teams/:id/open-roles` — открытые роли команды
- `GET /teams/:id/candidates` — кандидаты для команды
- `GET /teams/:teamId/candidates/:candidateId/recommendations` — рекомендации по кандидату

## Особенности

- Мобильная-first вёрстка
- Адаптивный дизайн
- Визуализация профилей DISC и Герчикова
- Цветовая индикация уровня совместимости
- Фильтрация кандидатов по ролям
