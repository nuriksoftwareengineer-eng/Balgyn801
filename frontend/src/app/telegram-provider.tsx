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
    if (!telegram.isTelegram() || loading || attempted.current) return;
    const initData = telegram.getInitData();
    if (!initData) return;
    attempted.current = true;

    // Best-effort and silent: this runs automatically on every Mini App launch, so surfacing
    // a failure here (e.g. Telegram already linked elsewhere) would be a confusing, unprompted
    // error. The explicit "Connect Telegram" profile action is where linking errors are shown.
    if (token) {
      linkTelegram(initData).catch(() => undefined);
    } else {
      loginWithTelegram(initData).catch(() => undefined);
    }
  }, [token, loading, loginWithTelegram, linkTelegram]);

  return <>{children}</>;
}
