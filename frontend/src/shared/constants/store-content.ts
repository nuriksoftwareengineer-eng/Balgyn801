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
export const CONTACT_EMAIL = "hello@balgyn.local";

/** Ссылка на Telegram магазина (или заглушка до публикации канала). */
export const STORE_TELEGRAM_URL = "https://t.me/balgyn_shop";
