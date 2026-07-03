# Микросервисы электронной подписи и шифрования (Java + Go)

Два независимых REST-микросервиса со встроенными криптографическими функциями
(**PKCS#7 / CMS**), реализующие одинаковый контракт API. Оба сервиса пишут исходные
данные и результаты операций в общую СУБД **PostgreSQL**.

Криптопровайдеры: **Bouncy Castle** (Java) и стандартная библиотека Go + `smallstep/pkcs7`.

## Реализованные методы

| Метод | Описание |
|---|---|
| Подпись PKCS#7 | CMS SignedData, `SHA256withRSA`, присоединённая (attached) и отсоединённая (detached) |
| Проверка PKCS#7 | проверка CMS-подписи, возврат субъекта сертификата подписанта |
| Шифрование | CMS EnvelopedData — гибрид **AES-256 + RSA** (сеансовый AES-ключ шифруется RSA) |
| Дешифрование | расшифрование CMS EnvelopedData закрытым ключом |
| Хэш | `SHA-256` / `SHA-512` / `PBKDF2WithHmacSHA256` |
| Загрузка по HTTPS | доверенное TLS-соединение с внешним ресурсом, скачивание документа, расчёт SHA-256 |

Дополнительно: TLS/HTTPS + опциональная взаимная аутентификация (**mTLS**), JUnit/Go-тесты,
пароли к контейнерам ключей вынесены в переменные окружения,
простой **Web UI** для Java-сервиса.

## Структура репозитория

```
docker-compose.yml        PostgreSQL + инициализация схемы
db/init.sql               таблица crypto_operation (общая)
scripts/gen-keys.sh       генерация ключей и сертификатов (OpenSSL + keytool)
certs/                    сгенерированные ключи/сертификаты
java-crypto-service/      Spring Boot 2.7 (Java 11), Bouncy Castle, Web UI
go-crypto-service/        Go, chi, smallstep/pkcs7, pgx
```

## Предусловия

- **JDK 11+**, **Maven**
- **Go 1.23+**
- **Docker** + Docker Compose
- **OpenSSL** и **keytool**

## Быстрый старт

### 1. Сгенерировать ключи и сертификаты

```bash
# пароль к keystore задаётся через переменную окружения (по умолчанию changeit)
export KEYSTORE_PASSWORD=changeit
bash scripts/gen-keys.sh
```

Будут созданы: удостоверяющий центр (CA), крипто-идентичность для подписи/шифрования
(`certs/crypto/crypto.p12` и `.jks`), серверный TLS-сертификат, клиентский сертификат для mTLS,
truststore.

### 2. Поднять PostgreSQL

```bash
docker compose up -d
```

СУБД поднимется на порту **5433** (хост) → 5432 (контейнер); схема создаётся из `db/init.sql`.
Порт можно изменить: `POSTGRES_PORT=5432 docker compose up -d`.

### 3. Запустить Java-сервис

```bash
cd java-crypto-service
mvn spring-boot:run
# либо: mvn -DskipTests package && java -jar target/java-crypto-service.jar
```

Сервис поднимется на `http://localhost:8080`. Web UI — `http://localhost:8080/`,
Swagger UI — `http://localhost:8080/swagger-ui.html`.


### 4. Запустить Go-сервис

```bash
cd go-crypto-service
go run ./cmd/server
```

Сервис поднимется на `http://localhost:8081`.

## Переменные окружения

**Общие:**

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `KEYSTORE_PASSWORD` | `changeit` | пароль ко всем keystore (используется в `gen-keys.sh`) |

**Java-сервис:**

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `SERVER_PORT` | `8080` | порт |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5433/crypto` / `crypto` / `crypto` | подключение к БД |
| `CRYPTO_KEYSTORE` / `CRYPTO_KEYSTORE_PASSWORD` / `CRYPTO_KEY_ALIAS` | `../certs/crypto/crypto.p12` / `changeit` / `crypto` | контейнер ключей для подписи/шифрования |
| `SERVER_SSL_ENABLED` | `false` | включить HTTPS |
| `TLS_KEYSTORE` / `TLS_KEYSTORE_PASSWORD` | `../certs/tls/server.p12` / `changeit` | серверный TLS-keystore |
| `SSL_CLIENT_AUTH` | `none` | `need` — включить mTLS |
| `TLS_TRUSTSTORE` / `TLS_TRUSTSTORE_PASSWORD` | `../certs/tls/truststore.p12` / `changeit` | truststore для mTLS |

**Go-сервис:**

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `SERVER_ADDR` | `:8081` | адрес |
| `DB_DSN` | `postgres://crypto:crypto@localhost:5433/crypto?sslmode=disable` | подключение к БД |
| `CRYPTO_CERT` / `CRYPTO_KEY` | `../certs/crypto/crypto.crt` / `.key` | ключи для подписи/шифрования |
| `TLS_ENABLED` / `TLS_CERT` / `TLS_KEY` | `false` / `../certs/tls/server.crt` / `.key` | HTTPS |
| `MTLS_ENABLED` / `CA_CERT` | `false` / `../certs/ca/ca.crt` | взаимная аутентификация |

## Примеры вызовов (curl)

`{BASE}` = `http://localhost:8080/api/v1` (Java) или `http://localhost:8081/api/v1` (Go).
Бинарные поля передаются в **base64**.

```bash
DATA=$(printf 'Hello Crypto' | base64)

# Хэш SHA-256
curl -s -X POST {BASE}/hash -H 'Content-Type: application/json' \
  -d "{\"data\":\"$DATA\",\"algorithm\":\"SHA-256\"}"

# Подпись (attached)
curl -s -X POST {BASE}/signatures/sign -H 'Content-Type: application/json' \
  -d "{\"data\":\"$DATA\",\"detached\":false}"

# Проверка attached-подписи
curl -s -X POST {BASE}/signatures/verify -H 'Content-Type: application/json' \
  -d "{\"signature\":\"<CMS base64>\",\"detached\":false}"

# Проверка detached-подписи (нужен исходный документ)
curl -s -X POST {BASE}/signatures/verify -H 'Content-Type: application/json' \
  -d "{\"signature\":\"<CMS base64>\",\"data\":\"$DATA\",\"detached\":true}"

# Шифрование / дешифрование
curl -s -X POST {BASE}/encryption/encrypt -H 'Content-Type: application/json' \
  -d "{\"data\":\"$DATA\"}"
curl -s -X POST {BASE}/encryption/decrypt -H 'Content-Type: application/json' \
  -d "{\"envelope\":\"<CMS base64>\"}"

# Загрузка документа по HTTPS
curl -s -X POST {BASE}/documents/fetch -H 'Content-Type: application/json' \
  -d '{"url":"https://raw.githubusercontent.com/git/git/master/README.md"}'
```

Каждый вызов создаёт запись в таблице `crypto_operation` (колонка `service` = `java` или `go`):

```bash
docker exec crypto-postgres psql -U crypto -d crypto -c \
  "select id, service, operation, status, created_at from crypto_operation order by id;"
```

## Web UI

Откройте `http://localhost:8080/` — можно ввести текст или загрузить файл и выполнить
подпись / шифрование / хэш, а также скачать по HTTPS документ по URL. Результат показывается
в base64 и доступен для скачивания.

## Тесты

**Java** (JUnit 5 — roundtrip подпись/проверка, шифрование/дешифрование, вектор SHA-256,
web-слой через MockMvc, интеграционный тест с Testcontainers):

```bash
cd java-crypto-service && mvn test
```

> Интеграционный тест на Testcontainers требует совместимого Docker и автоматически
> пропускается, если Docker недоступен.

**Go** (roundtrip подпись/проверка, шифрование/дешифрование, вектор SHA-256):

```bash
cd go-crypto-service && go test ./...
```

## TLS / HTTPS и mTLS

**Java** — HTTPS с проверкой клиентского сертификата:

```bash
cd java-crypto-service
SERVER_SSL_ENABLED=true SSL_CLIENT_AUTH=need mvn spring-boot:run

curl -sk https://localhost:8080/api/v1/hash ...
curl -sk --cert ../certs/client/client.crt --key ../certs/client/client.key \
     https://localhost:8080/api/v1/hash ...
```

**Go** — HTTPS/mTLS:

```bash
cd go-crypto-service
TLS_ENABLED=true MTLS_ENABLED=true go run ./cmd/server
curl -sk --cert ../certs/client/client.crt --key ../certs/client/client.key \
     https://localhost:8081/api/v1/hash ...
```