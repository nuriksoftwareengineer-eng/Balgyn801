import { useEffect, useRef, type ReactNode } from "react";
import { useAuth } from "@/app/auth-context";
import * as telegram from "@/shared/lib/telegram";

/**
 * Boots the Telegram SDK on mount and silently authenticates inside Telegram: logs in (or
 * registers) via initData if there's no local session yet, or links the verified Telegram
 * identity to the current session if one already exists. A pure pass-through outside Telegram.
 * Must render inside AuthProvider — it depends on useAuth().
 */
export function TelegramProvider({ children }: { children: ReactNode }) {
  const { token, loading, loginWithTelegram, linkTelegram } = useAuth();
  const attempted = useRef(false);

  useEffect(() => {
    telegram.init();
  }, []);

  useEffect(() => {
    // Regular browser traffic — the overwhelming majority of visits, and expected here.
    // Console-only: beaconing this to the backend on every normal page view would be noise.
    if (!telegram.isTelegram()) {
      console.debug("[Telegram] Not running inside Telegram Mini App — auto-auth skipped.");
      return;
    }
    if (loading || attempted.current) return;

    const initData = telegram.getInitData();
    if (!initData) {
      // Real Telegram context but no initData is genuinely unexpected — console-only, not
      // beaconed to the backend: this is a rare SDK/timing edge case, not worth its own
      // production endpoint just for observability.
      console.warn("[Telegram] Mini App context detected but initData is missing — auto-auth skipped.");
      return;
    }
    attempted.current = true;

    // Best-effort and silent: this runs automatically on every Mini App launch, so surfacing
    // a failure here (e.g. Telegram already linked elsewhere) would be a confusing, unprompted
    // error. The explicit "Connect Telegram" profile action is where linking errors are shown.
    if (token) {
      console.info("[Telegram] Already authenticated — linking Telegram identity instead of auto-login.");
      linkTelegram(initData).catch(() => undefined);
    } else {
      console.info("[Telegram] Attempting silent Telegram login.");
      loginWithTelegram(initData).catch(() => undefined);
    }
  }, [token, loading, loginWithTelegram, linkTelegram]);

  return <>{children}</>;
}
