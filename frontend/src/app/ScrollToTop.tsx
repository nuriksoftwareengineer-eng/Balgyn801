import { useEffect } from "react";
import { useLocation } from "react-router-dom";

/**
 * Сбрасывает скролл наверх при переходе между страницами.
 * Реагирует на смену pathname (не на hash — чтобы якорные ссылки работали).
 * Монтируется один раз в корневом layout, поэтому покрывает всю навигацию сайта,
 * а не отдельные кнопки.
 */
export function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    // Instant reset — стандарт для смены роутов: новая страница всегда
    // открывается сверху, без «залипания» на позиции предыдущей.
    window.scrollTo({ top: 0, left: 0, behavior: "auto" });
  }, [pathname]);

  return null;
}
