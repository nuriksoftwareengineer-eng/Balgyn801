package com.nurba.java.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a {@code .env} file from the working directory and returns its entries as a plain map.
 *
 * <p>Rules:
 * <ul>
 *   <li>Keys already present in the OS environment ({@link System#getenv}) are skipped —
 *       Docker / CI / production environment variables always win.</li>
 *   <li>Lines starting with {@code #} and blank lines are ignored.</li>
 *   <li>Optional leading {@code export } prefix is stripped.</li>
 *   <li>Values may be single- or double-quoted; inline {@code # comments} are trimmed otherwise.</li>
 *   <li>A missing or unreadable {@code .env} file is silently ignored.</li>
 * </ul>
 *
 * <p>Intentionally has zero Spring dependencies so it can be called from {@code main()}
 * before the Spring context is created — making it work in every deployment mode:
 * IDE, {@code ./gradlew bootRun}, {@code java -jar}, Docker, and docker-compose.
 */
public final class DotenvLoader {

    private static final String DEFAULT_FILENAME = ".env";

    private DotenvLoader() {}

    /**
     * Loads {@code .env} from the process working directory.
     *
     * @return key-value pairs ready to be passed to
     *         {@link org.springframework.boot.SpringApplication#setDefaultProperties}.
     *         Never {@code null}; empty map when file is absent or unreadable.
     */
    public static Map<String, Object> load() {
        Path envPath = Path.of(System.getProperty("user.dir", "."), DEFAULT_FILENAME);
        if (!Files.isRegularFile(envPath)) {
            return Collections.emptyMap();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(envPath);
        } catch (IOException e) {
            // Unreadable .env — fall through; missing JWT_SECRET will produce a clear error later.
            return Collections.emptyMap();
        }

        Map<String, String> osEnv = System.getenv();
        Map<String, Object> result = new LinkedHashMap<>();

        for (String raw : lines) {
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
            if (key.isEmpty()) {
                continue;
            }
            // OS environment variables take precedence — never override them.
            if (osEnv.containsKey(key)) {
                continue;
            }
            String value = parseValue(line.substring(eq + 1).strip());
            result.put(key, value);
        }

        return Collections.unmodifiableMap(result);
    }

    private static String parseValue(String raw) {
        if (raw.length() >= 2
                && ((raw.startsWith("\"") && raw.endsWith("\""))
                    || (raw.startsWith("'") && raw.endsWith("'")))) {
            return raw.substring(1, raw.length() - 1);
        }
        // Strip trailing inline comment (" # ...").
        int hash = raw.indexOf(" #");
        return hash >= 0 ? raw.substring(0, hash).strip() : raw;
    }
}
