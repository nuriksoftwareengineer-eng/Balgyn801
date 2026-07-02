export const CATEGORY_LABELS = [
  "Распродажа",
  "Игры",
  "Аниме",
  "Спорт",
  "Музыка",
  "Смотреть всё",
] as const;

/** Совпадает с `StoreCategories.VIEW_ALL` на бэкенде — «все категории». */
export const STORE_VIEW_ALL_CATEGORY: (typeof CATEGORY_LABELS)[number] =
  "Смотреть всё";

/** Категории для товара в админке (как на главной, без «Смотреть всё»). */
export const PRODUCT_CATEGORIES = CATEGORY_LABELS.filter(
  (c) => c !== STORE_VIEW_ALL_CATEGORY,
);

/** Почта для подписки и обратной связи — замените на боевой ящик команды. */
export const CONTACT_EMAIL = "zhakaevdias228@Gmail.com";

/** Telegram: прямой чат с магазином (поддержка / заказы). */
export const STORE_TELEGRAM_URL = "https://t.me/balgyncatalog";
export const SUPPORT_TELEGRAM_URL = "https://t.me/balgynbol";

/** Telegram Channel — канал для обновлений и дропов. */
export const TELEGRAM_CHANNEL_URL = "https://t.me/balgyn_channel";

/** WhatsApp номер магазина. */
export const WHATSAPP_URL = "https://wa.me/77001234567";
