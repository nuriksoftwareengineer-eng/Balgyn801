import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import ru from "../../public/locales/ru/translation.json";
import en from "../../public/locales/en/translation.json";
import kk from "../../public/locales/kk/translation.json";

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      ru: { translation: ru },
      en: { translation: en },
      kk: { translation: kk },
    },
    supportedLngs: ["ru", "kk", "en"],
    fallbackLng: "ru",
    defaultNS: "translation",
    detection: {
      order: ["localStorage", "navigator"],
      caches: ["localStorage"],
      lookupLocalStorage: "balgyn_lng",
    },
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;
