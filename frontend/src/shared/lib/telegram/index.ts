/**
 * Single seam between BALGYN and the Telegram Mini Apps SDK. The rest of the app must not
 * import `@telegram-apps/sdk` directly — every call here is a guarded no-op outside Telegram,
 * so callers never need their own `isTelegram()` branch.
 */
import {
  init as sdkInit,
  isTMA,
  mountMiniAppSync,
  setMiniAppHeaderColor,
  setMiniAppBackgroundColor,
  mountViewport,
  expandViewport,
  mountBackButton,
  isBackButtonMounted,
  showBackButton,
  hideBackButton,
  onBackButtonClick,
  mountMainButton,
  isMainButtonMounted,
  setMainButtonParams,
  onMainButtonClick,
  hapticFeedbackImpactOccurred,
  hapticFeedbackNotificationOccurred,
  hapticFeedbackSelectionChanged,
  isHapticFeedbackSupported,
  restoreInitData,
  initDataRaw,
  initDataUser,
  retrieveRawInitData,
} from "@telegram-apps/sdk";
import type { HapticFeedback, MainButtonOptions, TelegramUser } from "./types";

export type { HapticFeedback, MainButtonOptions, TelegramUser };

let initialized = false;

/** True inside Telegram's WebView (Mini App environment); false in a normal browser. */
export function isTelegram(): boolean {
  try {
    return isTMA();
  } catch {
    return false;
  }
}

/**
 * Boots the Telegram SDK exactly once per page load: wires event handling, restores initData,
 * mounts the Mini App + Viewport, expands to full height, and syncs Telegram's own chrome
 * (header/background) to the site's light palette. A pure no-op outside Telegram.
 */
export function init(): void {
  if (initialized || !isTelegram()) return;
  initialized = true;

  try {
    sdkInit();
    restoreInitData();
    if (mountMiniAppSync.isAvailable()) mountMiniAppSync();
    if (mountViewport.isAvailable()) void mountViewport();
    expand();
    applyLightChrome();
  } catch {
    // Telegram env detected but the SDK failed unexpectedly — the app must still render as a
    // normal page, so a startup hiccup here is swallowed rather than blocking the UI.
  }
}

/** Forces Telegram's native chrome (header/background) to the site's light palette, regardless
 *  of the user's Telegram theme setting — BALGYN intentionally has no dark theme. */
function applyLightChrome(): void {
  if (setMiniAppHeaderColor.isAvailable()) {
    setMiniAppHeaderColor(setMiniAppHeaderColor.supports.rgb() ? "#ffffff" : "bg_color");
  }
  if (setMiniAppBackgroundColor.isAvailable()) {
    setMiniAppBackgroundColor("#ffffff");
  }
}

/** Expands the Mini App to its maximum available height. No-op outside Telegram. */
export function expand(): void {
  if (!isTelegram() || !expandViewport.isAvailable()) return;
  expandViewport();
}

/** The Telegram user from initData, or null outside Telegram / before a valid launch. */
export function getUser(): TelegramUser | null {
  if (!isTelegram()) return null;
  const user = initDataUser();
  if (!user) return null;
  return {
    id: user.id,
    username: user.username,
    firstName: user.first_name,
    lastName: user.last_name,
    photoUrl: user.photo_url,
  };
}

/** The raw initData string to send to POST /auth/telegram for server-side verification. */
export function getInitData(): string | null {
  if (!isTelegram()) return null;
  return initDataRaw() ?? retrieveRawInitData() ?? null;
}

/** Fires Telegram's native haptic feedback. Always no-ops outside Telegram — never call the
 *  underlying SDK haptics directly from app code. */
export function haptic(kind: HapticFeedback): void {
  if (!isTelegram() || !isHapticFeedbackSupported()) return;
  try {
    if (kind === "selection") {
      if (hapticFeedbackSelectionChanged.isAvailable()) hapticFeedbackSelectionChanged();
    } else if (kind === "success" || kind === "warning" || kind === "error") {
      if (hapticFeedbackNotificationOccurred.isAvailable()) hapticFeedbackNotificationOccurred(kind);
    } else if (hapticFeedbackImpactOccurred.isAvailable()) {
      hapticFeedbackImpactOccurred(kind);
    }
  } catch {
    // Best-effort — haptics are a nicety, never worth surfacing an error for.
  }
}

/**
 * Shows/hides Telegram's native Back Button and wires a click handler.
 * @returns an unsubscribe function for the click handler, or undefined outside Telegram.
 */
export function backButton(visible: boolean, onClick?: () => void): VoidFunction | undefined {
  if (!isTelegram()) return undefined;
  if (mountBackButton.isAvailable() && !isBackButtonMounted()) mountBackButton();

  if (visible) {
    if (showBackButton.isAvailable()) showBackButton();
  } else if (hideBackButton.isAvailable()) {
    hideBackButton();
  }

  return onClick && onBackButtonClick.isAvailable() ? onBackButtonClick(onClick) : undefined;
}

/**
 * Configures Telegram's native Main Button, or hides it when passed null.
 * @returns an unsubscribe function for the click handler, or undefined outside Telegram.
 */
export function mainButton(options: MainButtonOptions | null): VoidFunction | undefined {
  if (!isTelegram()) return undefined;
  if (mountMainButton.isAvailable() && !isMainButtonMounted()) mountMainButton();

  if (!options) {
    if (setMainButtonParams.isAvailable()) setMainButtonParams({ isVisible: false });
    return undefined;
  }

  if (setMainButtonParams.isAvailable()) {
    setMainButtonParams({
      text: options.text,
      isVisible: true,
      isEnabled: options.isEnabled ?? true,
      isLoaderVisible: options.isLoaderVisible ?? false,
    });
  }
  return onMainButtonClick.isAvailable() ? onMainButtonClick(options.onClick) : undefined;
}
