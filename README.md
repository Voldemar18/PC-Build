# Мобильное приложение для сборки пк(Кт 6)

# Содержимое проекта
```
ui/ # Экраны и ViewModel (UI + логика)
├── auth/ # Вход / регистрация
│ ├── LoginScreen.kt
│ ├── RegisterScreen.kt
│ └── AuthViewModel.kt
├── home/ # Главный экран с навигацией
│ ├── HomeScreen.kt
│ └── HomeViewModel.kt
├── list/ # Мои сборки
│ ├── PcBuildListScreen.kt
│ └── PcBuildListViewModel.kt
├── community/ # Публичные сборки
│ ├── CommunityPcBuildListScreen.kt
│ └── CommunityPcBuildListViewModel.kt
├── favorites/ # Избранное
│ ├── FavoritesScreen.kt
│ └── FavoritesViewModel.kt
├── detail/ # Детали сборки
│ ├── PcBuildDetailScreen.kt
│ └── PcBuildDetailViewModel.kt
├── create/ # Создание сборки
│ ├── CreatePcBuildScreen.kt
│ ├── CreatePcBuildViewModel.kt
│ ├── ComponentSelectionViewModel.kt
│ └── StepComponentScreen.kt
├── edit/ # Редактирование сборки
│ ├── EditPcBuildScreen.kt
│ └── EditPcBuildViewModel.kt
└── settings/ # Настройки
├── SettingsScreen.kt
└── SettingsViewModel.kt

data/ # Работа с данными
├── database/ # Room (локальная БД)
│ ├── PcBuildDatabase.kt
│ ├── PcBuildDao.kt
│ ├── PcBuildEntity.kt
│ ├── FavoriteBuildDao.kt
│ ├── FavoriteBuildEntity.kt
│ └── Converters.kt
├── network/ # Retrofit (API)
│ ├── ApiService.kt
│ ├── AuthInterceptor.kt
│ ├── RefreshTokenInterceptor.kt
│ ├── AuthDTO.kt
│ └── PcBuildDTO.kt
├── local/ # Хранение данных на устройстве
│ ├── TokenManager.kt
│ ├── UserManager.kt
│ └── ThemeManager.kt
├── repository/ # Общий менеджер данных
│ ├── PcBuildRepository.kt
│ └── ProductRepository.kt

domain/ # Модели данных
├── models/
│ ├── PcBuild.kt
│ ├── PcBuildComponent.kt
│ └── UiState.kt

di/ # Hilt-модули (инъекция зависимостей)
├── DatabaseModule.kt
└── NetworkModule.kt

navigation/ # Навигация между экранами
└── NavGraph.kt

theme/ # Тема оформления
└── Theme.kt
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


## 🚀 Функциональные возможности

### Основные экраны (8+ экранов)
| Экран | Описание |
|-------|----------|
| **Login** | Вход в аккаунт |
| **Register** | Регистрация нового пользователя |
| **Home** | Главное меню со ссылками на разделы |
| **My Builds** | Список моих сборок (с поиском и обновлением) |
| **Community Builds** | Публичные сборки других пользователей |
| **Favorites** | Избранные сборки |
| **Build Details** | Детальная информация о сборке |
| **Create Build** | Пошаговое создание новой сборки |
| **Edit Build** | Редактирование существующей сборки |
| **Settings** | Тема оформления, информация о пользователе |

### Ключевые функции
- ✅ **Регистрация и авторизация** с JWT токенами
- ✅ **Автоматическое обновление токена** (RefreshToken)
- ✅ **Создание сборок ПК** с выбором компонентов по категориям
- ✅ **Редактирование и удаление** своих сборок
- ✅ **Избранное** — добавление/удаление публичных сборок
- ✅ **Поиск** по названию и автору (локальный и удаленный)
- ✅ **Офлайн-режим** — просмотр кэшированных сборок без интернета
- ✅ **Тёмная/светлая тема** с сохранением выбора
- ✅ **Состояния загрузки, ошибок и пустого списка**

## 🔄 Offline-First архитектура

Приложение спроектировано по принципу **offline-first**:
1. UI подписывается на `Flow` из **Room** (локальная БД)
2. При загрузке экрана сразу показываются кэшированные данные
3. Фоновый запрос к **API** обновляет БД
4. **StateFlow** автоматически обновляет UI при изменении данных
5. При ошибке сети показываются данные из кэша с индикатором

```kotlin
// Пример из PcBuildListViewModel
private fun loadData() {
    _uiState.value = UiState.Loading
    
    // 1. Показываем кэш
    repository.getMyBuilds().collect { builds ->
        if (builds.isNotEmpty()) {
            _uiState.value = UiState.Success(builds)
        }
    }
    
    // 2. Обновляем из API в фоне
    refreshFromApi()
}
```

## Состояния UI (UiState)

```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
```

Каждый экран обрабатывает все 4 состояния:

⏳ Loading — крутилка, ожидание данных

✅ Success — отображение списка/детальной информации

❌ Error — сообщение об ошибке + кнопка Retry

📭 Empty — сообщение о пустом списке + действие


# 🔧 Запуск проекта
### Требования
Android Studio Koala или новее

JDK 17

Android SDK (minSdk 26, targetSdk 34)

Запущенный бэкенд-сервер (см. документацию API)

### Инструкция
1 Клонировать репозиторий
```
git clone <your-repo-url>
```
2 Открыть проект в Android Studio

3 Настроить BASE_URL в NetworkModule.kt:
```
// Для эмулятора (localhost сервера)
private const val BASE_URL = "http://10.0.2.2:8080/"

// Для физического устройства (замените на IP вашего компьютера)
private const val BASE_URL = "http://192.168.1.100:8080/"
```
4 Синхронизировать проект (File → Sync Project with Gradle Files)

5 Запустить приложение (Run 'app')
