package com.nurba.java.enums;

/**
 * Режим провайдера доставки CDEK. Переключается только через конфигурацию
 * ({@code cdek.provider}), без правок кода.
 */
public enum CdekProviderMode {
    /** real, если заданы CLIENT_ID/CLIENT_SECRET, иначе mock. Значение по умолчанию. */
    AUTO,
    /** Принудительно mock (заглушки), даже при наличии ключей — для отладки. */
    MOCK,
    /** Принудительно real CDEK API (требует ключей). */
    REAL
}
