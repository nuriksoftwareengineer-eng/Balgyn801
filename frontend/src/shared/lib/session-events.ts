/**
 * Событие полной очистки пользовательской сессии (logout).
 * AuthProvider диспатчит его на window; провайдеры с собственным состоянием
 * (CartProvider) подписываются и сбрасывают свои данные. Так logout чистит
 * корзину без перестройки дерева провайдеров и циклических импортов.
 */
export const SESSION_CLEARED_EVENT = "balgyn:session-cleared";

export function dispatchSessionCleared(): void {
  window.dispatchEvent(new Event(SESSION_CLEARED_EVENT));
}
