# Мобильное приложение для сборки пк(Кт 5)

# Содержимое проекта
```
ui/ # Экраны и ViewModel (UI + логика)
├── auth/ # Вход / регистрация
├── list/ # Список сборок
├── detail/ # Детали сборки
├── create/ # Создание сборки
└── edit/ # Редактирование

data/  # Работа с данными
├── database/ # Room (локальная БД)
├── network/ # Retrofit (API)
├── local/ # TokenManager (хранение токенов)
└── repository/ # Общий менеджер данных

domain/ # Модели данных (сборки, компоненты)

di/ # Hilt-модули (инъекция зависимостей)

navigation/ # Навигация между экранами
```

# API

### Авторизация
- `POST /api/register` — регистрация
- `POST /api/login` — вход
- `POST /api/auth/refresh` — обновление токена
- `POST /api/auth/logout` — выход

### Сборки
- `GET /api/pc-builds/public` — публичные сборки
- `GET /api/pc-builds/public/{id}` — детали публичной
- `GET /api/pc-builds/me` — мои сборки
- `GET /api/pc-builds/me/{id}` — моя сборка по ID
- `POST /api/pc-builds/me` — создать сборку
- `PUT /api/pc-builds/me/{id}` — обновить
- `DELETE /api/pc-builds/me/{id}` — удалить
- `POST /api/pc-builds/me/{id}/components/{productId}` — добавить компонент
- `DELETE /api/pc-builds/me/{id}/components/{productId}` — удалить компонент
- `GET /api/pc-builds/me/{id}/total` — стоимость сборки
- `GET /api/pc-builds/me/{id}/components` — список компонентов

### Комплектующие
- `GET /api/products` — все товары
- `GET /api/products/search` — поиск
- `GET /api/products/category/{categoryId}` — по категории

### Категории и типы
- `GET /api/component-types` — типы компонентов
- `GET /api/component-types/ordered` — с сортировкой
- `GET /api/categories` — категории
- `GET /api/categories/tree` — дерево категорий

# Технологии

## 🛠 Технологический стек

| Технология         | Назначение |
|--------------------|-------------|
| **Jetpack Compose** | Декларативный UI (без XML) |
| **Material 3**      | Готовые стилизованные компоненты |
| **ViewModel**       | Хранение данных при поворотах экрана |
| **StateFlow**       | Автоматическое обновление UI при изменении данных |
| **Room**            | Локальная база данных (кэш сборок) |
| **Retrofit**        | HTTP-запросы к серверу |
| **Coroutines**      | Асинхронность без блокировки UI |
| **Hilt**            | Внедрение зависимостей (без `new`) |

