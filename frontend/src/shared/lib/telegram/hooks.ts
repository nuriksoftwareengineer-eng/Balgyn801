import { useEffect, useRef } from "react";
import { backButton, isTelegram, mainButton } from "./index";
import type { MainButtonOptions } from "./types";

/**
 * Shows Telegram's native Back Button whenever `visible` is true, wired to `onBack`.
 * A no-op outside Telegram — browser back-navigation (React Router) is untouched there.
 */
export function useTelegramBackButton(visible: boolean, onBack: () => void): void {
  const onBackRef = useRef(onBack);
  onBackRef.current = onBack;

  useEffect(() => {
    if (!isTelegram()) return undefined;
    const unsubscribe = backButton(visible, () => onBackRef.current());
    return () => unsubscribe?.();
  }, [visible]);
}

/**
 * Configures Telegram's native Main Button for as long as the calling component is mounted with
 * non-null options; hides it on unmount or when passed null. A no-op outside Telegram.
 */
export function useTelegramMainButton(options: MainButtonOptions | null): void {
  const onClickRef = useRef(options?.onClick);
  onClickRef.current = options?.onClick;

  const visible = options !== null;
  const text = options?.text;
  const isEnabled = options?.isEnabled;
  const isLoaderVisible = options?.isLoaderVisible;

  useEffect(() => {
    if (!isTelegram()) return undefined;

    const unsubscribe =
      visible && text
        ? mainButton({ text, isEnabled, isLoaderVisible, onClick: () => onClickRef.current?.() })
        : mainButton(null);

    return () => {
      unsubscribe?.();
      mainButton(null);
    };
  }, [visible, text, isEnabled, isLoaderVisible]);
}
