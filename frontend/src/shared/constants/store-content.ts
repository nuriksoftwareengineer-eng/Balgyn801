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
export const CONTACT_EMAIL = "balgyn.studio@gmail.com";

/** Telegram: прямой чат с магазином (поддержка / заказы). */
export const STORE_TELEGRAM_URL = "https://t.me/balgynbol";
export const SUPPORT_TELEGRAM_URL = "https://t.me/balgynbol";

/** Telegram Channel — канал для обновлений и дропов. */
export const TELEGRAM_CHANNEL_URL = "https://t.me/balgyncatalog";

/** WhatsApp номер магазина. */
export const WHATSAPP_URL = "https://wa.me/77081937510";

/** Instagram магазина. */
export const INSTAGRAM_URL = "https://instagram.com/balgyn.bol";

/** Телефон магазина (для отображения / mailto-tel). */
export const CONTACT_PHONE = "+7 708 193 75 10";
export const CONTACT_PHONE_HREF = "+77081937510";

/**
 * Реквизиты индивидуального предпринимателя (обязательны для комплаенса Freedom Pay).
 * Единый источник правды — используется в футере, на «Контактах» и в «Публичной оферте».
 * Значения — юридические данные ИП; переводятся только подписи (labels) через i18n.
 */
export const MERCHANT = {
  /** Individual entrepreneur legal name. */
  ieName: "Balgyn bol",
  /** Owner full name. */
  owner: "Әбріс Диас Нұржанұлы",
  /** Individual Identification Number. */
  iin: "060503500028",
  /** Legal address, one component per line. */
  addressLines: [
    "Республика Казахстан",
    "Алматинская область",
    "Жамбылский район",
    "село Каргалы",
    "улица Абая, 50",
  ],
  email: CONTACT_EMAIL,
  phone: CONTACT_PHONE,
  phoneHref: CONTACT_PHONE_HREF,
} as const;
