# FINAL_SECURITY_REVIEW

**Дата:** 2026-06-21  
**Область:** MinIO buckets · File upload · Public file access · MIME validation · File size limits · Admin bootstrap

---

## Итог

**Критических уязвимостей не найдено.**  
Найдены 2 средних риска — оба связаны с дефолтными учётными данными, которые должны быть заменены перед деплоем в прод.

---

## 1. MinIO Buckets

**Файл:** `src/main/java/.../config/MinioBucketInitializer.java`

Бакет создаётся при старте, если не существует. Bucket policy:

```json
{
  "Effect": "Allow",
  "Principal": {"AWS": ["*"]},
  "Action": ["s3:GetObject"],
  "Resource": ["arn:aws:s3:::balgyn-media/*"]
}
```

| Проверка | Результат |
|----------|-----------|
| Публичное чтение объектов (`s3:GetObject`) | ✅ Намеренно — изображения товаров публичны |
| Публичная запись (`s3:PutObject`) | ✅ Отсутствует |
| Публичное удаление (`s3:DeleteObject`) | ✅ Отсутствует |
| Листинг директорий (`s3:ListBucket`) | ✅ Отсутствует |
| Запись только через аутентифицированный backend | ✅ `@PreAuthorize("hasRole('ADMIN')")` |

**⚠ MEDIUM — Default MinIO credentials**

`application.properties` строки 51–52:
```
app.storage.access-key=${MINIO_ROOT_USER:minioadmin}
app.storage.secret-key=${MINIO_ROOT_PASSWORD:minioadmin}
```
Если `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` не заданы в окружении, MinIO запускается с `minioadmin:minioadmin`.  
**Риск:** если MinIO-порт (9000/9001) открыт наружу, злоумышленник получает полный доступ к хранилищу.  
**Действие перед продом:** задать сильные уникальные значения `MINIO_ROOT_USER` и `MINIO_ROOT_PASSWORD` в `.env`; закрыть порты 9000 и 9001 фаерволом (nginx проксирует только `/api/v1` и статику).

---

## 2. File Upload Security

**Файлы:** `MediaUploadController.java` · `MinioMediaStorageService.java`

| Проверка | Результат |
|----------|-----------|
| Доступ только для ADMIN | ✅ `@PreAuthorize("hasRole('ADMIN')")` |
| Rate limit на upload | ✅ 5 запросов/мин (`SecurityRateLimitProperties.uploadPerMinute = 5`) |
| Путь загрузки (`folder`) задан в коде | ✅ `"products"` — не пользовательский ввод |
| Path traversal в имени файла | ✅ UUID-имя; оригинальное имя используется только для расширения |
| Расширение файла — санитизация | ✅ `ext.matches("\\.[a-z0-9]+")` + длина ≤ 8 символов |

---

## 3. Public File Access

Файлы отдаются через MinIO по URL вида:  
`{MINIO_PUBLIC_URL}/{bucket}/{UUID}.jpg`

| Проверка | Результат |
|----------|-----------|
| Листинг объектов в бакете | ✅ `s3:ListBucket` не в публичной политике |
| Запись через публичный URL | ✅ Только `s3:GetObject` |
| Cross-origin: MinIO на отдельном домене/порту | ✅ XSS из файлов не затрагивает домен приложения |

---

## 4. MIME Validation

**Файл:** `MinioMediaStorageService.java` строки 38–91

Двухуровневая проверка:

**Уровень 1 — Content-Type заголовок** (слабый сам по себе):
```java
if (!contentType.toLowerCase().startsWith("image/")) { throw ... }
```

**Уровень 2 — Magic bytes (реальные байты файла)**:

| Формат | Сигнатура | Проверяется |
|--------|-----------|-------------|
| JPEG | `FF D8 FF` | ✅ |
| PNG | `89 50 4E 47` | ✅ |
| GIF87a/GIF89a | `47 49 46 46 38` | ✅ |
| WebP | `52 49 46 46 .. .. .. .. 57 45 42 50` | ✅ |
| SVG | — | ✅ Заблокирован (нет magic bytes, `startsWith("image/svg")` не достигает magic) |
| PHP/EXE/PDF | — | ✅ Заблокированы magic bytes |

Комбинация Content-Type + magic bytes предотвращает загрузку исполняемых файлов даже при подмене заголовка клиентом.

---

## 5. File Size Limits

| Слой | Лимит |
|------|-------|
| Spring `max-file-size` (строка 56 application.properties) | 8 MB |
| Spring `max-request-size` (строка 57) | 9 MB |
| Приложение `MAX_BYTES` (строка 28 MinioMediaStorageService) | 8 MB |
| Rate limit (все слои) | 5 запросов/мин |

Лимиты согласованы. Расхождение `max-request-size = 9MB` при `max-file-size = 8MB` — стандартная практика (запрос = файл + заголовки multipart). Не является уязвимостью.

---

## 6. Admin Bootstrap Account

**Файл:** `src/main/java/.../config/BootstrapAdminInitializer.java`

| Проверка | Результат |
|----------|-----------|
| Запускается только если email И пароль не пусты | ✅ строки 34–36 |
| Не создаёт дубликата если пользователь существует | ✅ `existsByEmailIgnoreCase(email)` строка 37 |
| Пароль хранится bcrypt-хешем | ✅ `passwordEncoder.encode(password)` строка 43 |
| Email нормализован (lowercase) | ✅ строка 31 |

**⚠ MEDIUM — Default bootstrap credentials in .env.example**

`.env.example` строки 16–17:
```
BOOTSTRAP_ADMIN_EMAIL=admin@balgyn.local
BOOTSTRAP_ADMIN_PASSWORD=admin12345
```
Если производственный `.env` скопирован из `.env.example` и не изменён, admin-аккаунт будет создан с предсказуемым паролем `admin12345`.  
**Риск:** подбор пароля или компрометация при утечке `.env`.  
**Действие перед продом:**
1. Изменить `BOOTSTRAP_ADMIN_EMAIL` на реальный email
2. Изменить `BOOTSTRAP_ADMIN_PASSWORD` на случайный (≥ 20 символов, например: `openssl rand -base64 24`)
3. После первого логина и создания постоянного admin-аккаунта — удалить или очистить эти переменные из `.env`

---

## Сводка рисков

| # | Риск | Уровень | Действие |
|---|------|---------|----------|
| 1 | MinIO default credentials (`minioadmin:minioadmin`) | **MEDIUM** | Задать сильные `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` в `.env`; закрыть порты 9000/9001 |
| 2 | Bootstrap admin password `admin12345` в `.env.example` | **MEDIUM** | Изменить перед деплоем; очистить переменную после создания аккаунта |
| 3 | GIF polyglot (XSS в старых браузерах через GIF с JS) | **LOW** | Несущественно — MinIO на отдельном origin; современные браузеры не исполняют |
| 4 | Без проверки минимальной длины пароля при bootstrap | **LOW** | Приемлемо — env-переменная под контролем оператора |

**Критических уязвимостей нет.**  
Реализация upload security (magic bytes + Content-Type + rate limit + ADMIN-only + UUID filename) надёжна.
