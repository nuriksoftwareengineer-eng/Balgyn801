package com.nurba.java.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Загружает локальный файл {@code .env} (из рабочей директории) в System properties ДО старта Spring,
 * чтобы плейсхолдеры вида {@code ${JWT_SECRET}} резолвились без ручного экспорта переменных в
 * PowerShell/Linux.
 *
 * <p>Вызывается из {@code main()} — поэтому работает одинаково и в exploded-классах ({@code gradlew
 * bootRun}/IDE), и в fat-jar ({@code java -jar app.jar}): не зависит от расположения ресурсов в архиве.</p>
 *
 * <p>Приоритет: реальные переменные окружения ОС и уже заданные {@code -D} имеют приоритет над {@code .env}
 * (значение из файла применяется только если ключ ещё не задан). Это значит, что Docker/CI/прод-окружение
 * всегда переопределяют локальный файл. Отсутствующий {@code .env} молча игнорируется.</p>
 *
 * <p>Это инфраструктура загрузки конфигурации — НЕ меняет безопасность JWT: секрет по-прежнему обязателен
 * (дефолта в коде нет), меняется только источник его значения.</p>
 */
public final class DotenvBootstrap {

    private static final String DEFAULT_FILENAME = ".env";

    private DotenvBootstrap() {
    }

    /** Читает {@code ./.env} и переносит пары KEY=VALUE в System properties, не перетирая ENV ОС и {@code -D}. */
    public static void apply() {
        Path envPath = Path.of(System.getProperty("user.dir", "."), DEFAULT_FILENAME);
        if (!Files.isRegularFile(envPath)) {
            return; // .env нет (Docker/CI/прод используют реальные ENV) — молча выходим.
        }
        try {
            for (String raw : Files.readAllLines(envPath)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).strip();
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).strip();
                String value = unquote(line.substring(eq + 1).strip());
                if (key.isEmpty()) {
                    continue;
                }
                // Реальные ENV ОС и явные -D имеют приоритет над .env.
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            // Нечитаемый .env — игнорируем; отсутствие JWT_SECRET позже даст понятную ошибку в JwtService.
            System.err.println("[dotenv] Не удалось прочитать .env: " + e.getMessage());
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        int hash = value.indexOf(" #");
        if (hash >= 0) {
            value = value.substring(0, hash).strip();
        }
        return value;
    }
}
