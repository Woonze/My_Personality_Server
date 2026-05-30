# MyPersonality Server

Backend сервиса подбора персонала на `Ktor`.

## Стек

- Kotlin
- Ktor
- Exposed
- HikariCP
- PostgreSQL Neon

## Возможности

- регистрация и вход по `email/password`
- вакансии
- избранное
- отклики
- профиль
- каскадное удаление откликов и избранного при удалении вакансии

## Переменные окружения

- `DATABASE_URL` — строка подключения к Neon/PostgreSQL

Пример:

```text
postgresql://USER:PASSWORD@HOST/DBNAME?sslmode=require&channel_binding=require
```

## Запуск

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' GRADLE_USER_HOME=/tmp/gradle-home DATABASE_URL='<your_neon_url>' ./gradlew :server:run
```

## Тесты

```bash
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' GRADLE_USER_HOME=/tmp/gradle-home ./gradlew :server:test
```
