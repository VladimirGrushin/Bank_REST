# Bank REST API
RESTful API для банковской системы с полным циклом управления клиентами, счетами и транзакциями. Реализована JWT аутентификация и полная документация API.

## Содержание
- [Технологии](#технологии)
- [Начало работы](#начало-работы)
- [Тестирование](#тестирование)
- [Deploy и CI/CD](#deploy-и-ci/cd)
- [Contributing](#contributing)
- [To do](#to-do)
- [Команда проекта](#команда-проекта)

## Технологии
- **Java 17** - основной язык разработки
- **Spring Boot 3.1.5** - фреймворк
- **PostgreSQL 15** - база данных
- **Spring Data JPA** - работа с БД
- **Spring Security** - безопасность
- **JWT** - аутентификация
- **Docker & Docker Compose** - контейнеризация
- **Maven** - сборка проекта

##  Предварительные требования

Перед запуском убедитесь, что у вас установлены:

| Технология | Версия | Ссылка |
|------------|---------|--------|
| Java JDK | 17+ | [Download](https://openjdk.org/) |
| Maven | 3.6+ | [Download](https://maven.apache.org/) |
| Docker | 20.10+ | [Download](https://www.docker.com/) |
| Docker Compose | 2.0+ | [Download](https://docs.docker.com/compose/) |
| PostgreSQL | 15+ | [Download](https://www.postgresql.org/) |

##  Быстрый запуск

### Вариант 1: Docker Compose (рекомендуемый)

1. **Клонируйте репозиторий**
```bash
$ git clone https://github.com/VladimirGrushin/Bank_REST.git
$ cd Bank_REST
```
2. **Создайте файл .env**
```bash 
$ cat > .env << EOF
$ DB_URL=jdbc:postgresql://db:5432/bank_db
$ DB_USERNAME=bank_user
$ DB_PASSWORD=bank_password
$ JWT_SECRET=your-super-secret-jwt-key-min-256-bits-make-it-very-long-and-secure
$ JWT_EXPIRATION=86400000
$ EOF
```
3. **Запустите приложение**
```bash 
$ docker-compose up -d
```
Приложение будет доступно по адресу: http://localhost:8080

### Вариант 2: Ручная установка
1. **Создайте базу данных PostgreSQL**
```sql
$ CREATE DATABASE bank_card_db;
$ CREATE USER bank_user WITH PASSWORD 'bank_password';
$ GRANT ALL PRIVILEGES ON DATABASE bank_db TO bank_user;
```
2. **Настройте переменные окружения**
```bash
# Создайте .env файл
echo "DB_URL=jdbc:postgresql://localhost:5432/bank_db" > .env
echo "DB_USERNAME=bank_user" >> .env
echo "DB_PASSWORD=bank_password" >> .env
echo "JWT_SECRET=your-jwt-secret-key-256-bits-minimum" >> .env
echo "JWT_EXPIRATION=86400000" >> .env
```
3. **Соберите и запустите приложение**
```bash
# Сборка
mvn clean package

# Запуск
java -jar target/bank-rest-0.0.1-SNAPSHOT.jar

# Или через Maven
mvn spring-boot:run
```
## Тестирование
**Запуск всех тестов**
```bash
mvn test
```
**Запуск конкретного теста**
```bash
mvn test -Dtest="ClientServiceUnitTest"
```
**Запуск тестов в Docker**
```bash
docker-compose -f docker-compose.test.yml up --build
```

## Swagger UI / OpenAPI документация

### После запуска приложения доступна интерактивная документация API:

**Основная документация**

Swagger UI: http://localhost:8080/swagger-ui.html

OpenAPI JSON: http://localhost:8080/v3/api-docs

OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml

### Как использовать Swagger UI:
1.**Откройте http://localhost:8080/swagger-ui.html в браузере**

2.**Авторизуйтесь через endpoints аутентификации**

3.**Получите JWT токен из ответа /api/auth/login**

4.**Нажмите кнопку "Authorize" в правом верхнем углу**

5.**Введите токен в формате: Bearer ваш_jwt_токен**

6.**Тестируйте endpoints прямо из браузера**

### Пример работы с аутентификацией:
1.**Сначала зарегистрируйте пользователя через /api/auth/register**

2.**Выполните вход через /api/auth/login чтобы получить JWT токен**

3.**Используйте полученный токен для доступа к защищенным endpoints**

## Конфигурация

### Переменные окружения

**Создайте файл .env в корне проекта со следующими параметрами:**

| Переменная | Обязательное | Описание                 | Пример                                |
|------------|--------------|--------------------------|---------------------------------------|
| DB_URL | Да           | URL базы данных          | jdbc:postgresql://localhost:5432/bank_card_db |
| DB_USERNAME | Да           | Пользователь БД          | bank_user                             |
| DB_PASSWORD | Да           | Пароль БД                | bank_password                         |
| JWT_SECRET | Да           | Секретный ключ JWT       | your-secret-key-256-bits              |
| JWT_EXPIRATION | Нет          | Время жизни токена (мс)	 | 86400000                              |
| SPRING_PROFILES_ACTIVE| Нет          | Активные профили Spring  | docker                               |


### Порт приложения
**По умолчанию приложение запускается на порту 8080. Для изменения порта используйте:**
```application.yml
server.port=9090
```

## API Endpoints

### Аутентификация
| Метод	 | Endpoint           | Описание |
|--------|--------------------|----------|
| POST	  | /api/auth/login    |	Вход в систему |
| POST   | /api/auth/register | Регистрация пользователя |
| POST | /api/auth/register/admin | Регистрация админа |
| POST | /api/auth/logout| Выход из системы |

### Клиенты
| Метод	 | Endpoint                  | Описание                                       |
|--------|---------------------------|------------------------------------------------|
| GET    | /api/users/me             | 	Получить свой профиль                         |
| PATCH  | /api/users/me/password    | Изменить свой пароль                           |
| GET    | /api/users/all            | Получить список всех пользователей(для админа) |
| POST   | /api/users                | Создать пользователя(для админа)               |
| DELETE | /api/users/{id}           | Удалить пользователя(для админа)               |
| PATCH | /api/usrs/{id}/role       | Изменить роль пользователя(для админа)         |
| GET | /api/users/search         | Найти пользователя по имени(для админа)        |
| GET | /api/users/{id}           | Найти пользователя по id(для админа)           |
| GET | /api/users/by-role/{role} | Найти пользователей по роли(для админа)        |

### Банковские карты
| Метод	 | Endpoint                                | Описание                           |
|--------|-----------------------------------------|------------------------------------|
| GET    | /api/cards/my                           | 	Получить список своих карт        |
| POST   | /api/cards/admin/create                 | Создать карту(для админа)          |
| GET    | /api/cards/admin/{cardId}/number        | Получить номер карты(для админа)   |
| PATCH  | /api/cards/admin/{cardID}/activate      | Активировать карту(для админа)     |
| DELETE | /api/cards/admin/{cardId}               | Удалить карту(для админа)          |
| PATCH  | /api/cards/admin/{cardId}/approve-block | Подтвердить блокировку(для админа) |
| PATCH  | /api/cards/admin/{cardId}/reject-block  | Отменить блокировку(для админа)    |
| PATCH  | /api/cards/admin/{cardId}/block         | Заблокировать(для админа)          |
| PATCH   | /api/cards/{cardId}/request-block       | Запросить блокировку карты         |
| PATCH | /api/cards/{cardId}/cancel-block-request | Отменить запрос на блокировку |
| GET | /api/cards/{cardId}/balance | Просмотреть баланс своей карты |
| GET | /api/cards/{cardId} | Получить данные своей карты |
| GET | /api/cards/admin/all | Получить список всех карт(для админа) |
| GET | /api/cards/admin/status/{status} | Получить список карт по статусу(для админа) |

### Транзакции 

| Метод	 | Endpoint                            | Описание                                   |
|--------|-------------------------------------|--------------------------------------------|
| GET    | /api/transactions/my                       | 	Получить транзакции текущего пользователя |
| POST   | /api/transactions/transfer/my-cards | Перевод между своими картами               |

## Docker 
**Установите в файле docker-compose.yml следующие параметры(пример):**
```
POSTGRES_DB: bank_card_db
POSTGRES_USER: bank_user
POSTGRES_PASSWORD: bank_password
```
**Сборка образа**
```bash
docker build -t bank-rest-api .
```

**Запуск контейнера**
```bash
docker run -d -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/bank_card_db \
  -e DB_USERNAME=bank_user \
  -e DB_PASSWORD=bank_password \
  -e JWT_SECRET=your-jwt-secret \
  -e JWT_EXPIRATION=86400000 \
  --name bank-api \
  bank-rest-api
```

## Примеры использования
**Регистрация пользователя**
```bash
curl -X POST http://localhost:8080/api/auth/register \
-H "Content-Type: application/json" \
-d '{
"firstName": "Test",
"lastName": "User",
"password": "password123"
}'
```

**Вход в систему**
```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
"firstName": "Test",
"lastName": "User",
"password": "password123"
}'
```

## Частые проблемы

**Ошибка подключения к БД**
```bash
# Проверьте подключение к PostgreSQL
psql -h localhost -U bank_user -d bank_db
```

**Порт 8080 занят**

**Linux/macOS**
```bash
#Найти процесс:
lsof -i :8080
# или
lsof -i :8080 -t

#Завершить процесс:
kill -9 $(lsof -t -i :8080)
# или
lsof -ti:8080 | xargs kill -9
```
***Windows***
```bash
#Найти процесс:
netstat -ano | findstr :8080
```

```cmd
#Завершить процесс:
taskkill /PID <PID> /F
```

## Логирование
```bash
# Просмотр логов Docker
docker logs bank-api

# Логи приложения
tail -f logs/application.log
```

## Контакты
Владимир Грушин - [GitHub](https://github.com/VladimirGrushin)

Почта- klasniy.chuvachok@gmail.com

Ссылка на проект: https://github.com/VladimirGrushin/Bank_REST