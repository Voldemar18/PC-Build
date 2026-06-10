# Мобильное приложение для сборки пк(Кт 5)

# Содержимое проекта
```
ui/ # Экраны и ViewModel (UI + логика)
├── auth/ # Вход / регистрация
```
<img width="186" height="108" alt="2026-05-22_00-55-55" src="https://github.com/user-attachments/assets/41da7440-2a3e-48f3-9f1e-452a29e1108e" />

```
├── list/ # Список сборок
```
<img width="253" height="82" alt="image" src="https://github.com/user-attachments/assets/eff0a8f0-a058-45f1-9f97-3f80c3cf3fa8" />

```
├── detail/ # Детали сборки
```
<img width="250" height="70" alt="image" src="https://github.com/user-attachments/assets/f9c8a9d2-a918-45e8-9a20-4af03b4121d8" />

```
├── create/ # Создание сборки
```
<img width="298" height="125" alt="image" src="https://github.com/user-attachments/assets/12862eaa-b9a0-468c-a03f-f814ffffd56d" />

```
└── edit/ # Редактирование
```
<img width="253" height="79" alt="image" src="https://github.com/user-attachments/assets/0e750a66-3a47-487e-a81e-c49ba6de7e65" />

```
data/  # Работа с данными
├── database/ # Room (локальная БД)
```
<img width="180" height="128" alt="image" src="https://github.com/user-attachments/assets/0a1fed36-6648-4356-a59d-1c9cede9ddbb" />

```
├── network/ # Retrofit (API)
```
<img width="264" height="167" alt="image" src="https://github.com/user-attachments/assets/f4e1e497-bbc9-48de-a0c6-a9c89c184955" />

```
├── local/ # TokenManager (хранение токенов)
```
<img width="156" height="59" alt="image" src="https://github.com/user-attachments/assets/6e9e1b04-e519-4d24-99f5-c0594a890b78" />

```
└── repository/ # Общий менеджер данных
```
<img width="189" height="71" alt="image" src="https://github.com/user-attachments/assets/5f0f5a14-8bf6-477b-b28b-93d654e58aba" />

```
domain/ # Модели данных (сборки, компоненты)
```
<img width="198" height="101" alt="image" src="https://github.com/user-attachments/assets/58bc13bc-1a88-42c5-9856-4d626a73b8e2" />

```
di/ # Hilt-модули (инъекция зависимостей)
```
<img width="183" height="77" alt="image" src="https://github.com/user-attachments/assets/f5d79eb3-6f22-47ad-b609-e5a6919962b2" />

```
navigation/ # Навигация между экранами
```
<img width="155" height="41" alt="image" src="https://github.com/user-attachments/assets/2bf040a5-179e-47d5-a82a-055b8559e73c" />


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

