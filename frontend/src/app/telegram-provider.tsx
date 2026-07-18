import { useEffect, useRef, type ReactNode } from "react";
import { useAuth } from "@/app/auth-context";
import * as telegram from "@/shared/lib/telegram";

/**
 * Boots the Telegram SDK on mount and silently authenticates inside Telegram: logs in (or
 * registers) via initData if there's no local session yet, or links the verified Telegram
 * identity to the current session if one already exists. A pure pass-through outside Telegram.
 * Must render inside AuthProvider — it depends on useAuth().
 *
 * TEMP-DEBUG: this file has extra `[TEMP-DEBUG]` console.log lines added to diagnose a
 * production report of auto-login not completing inside real Telegram. They only add
 * visibility — no branch, condition, or call below was changed. Remove all `[TEMP-DEBUG]`
 * lines once the investigation concludes.
 */
export function TelegramProvider({ children }: { children: ReactNode }) {
  const { token, loading, loginWithTelegram, linkTelegram } = useAuth();
  const attempted = useRef(false);

  useEffect(() => {
    console.log("[TEMP-DEBUG] TelegramProvider mounted");
    telegram.init();
  }, []);

  useEffect(() => {
    const inTelegram = telegram.isTelegram();
    console.log("[TEMP-DEBUG] isTelegram:", inTelegram, "| loading:", loading, "| token:", !!token);

    // Regular browser traffic — the overwhelming majority of visits, and expected here.
    // Console-only: beaconing this to the backend on every normal page view would be noise.
    if (!inTelegram) {
      console.debug("[Telegram] Not running inside Telegram Mini App — auto-auth skipped.");
      return;
    }
    if (loading || attempted.current) {
      console.log("[TEMP-DEBUG] Skipping this pass — loading:", loading, "| attempted:", attempted.current);
      return;
    }

    const initData = telegram.getInitData();
    console.log("[TEMP-DEBUG] getInitData() full string:", initData);
    console.log("[TEMP-DEBUG] getInitData() length:", initData?.length ?? 0);
    console.log("[TEMP-DEBUG] getInitData() contains '+':", initData?.includes("+") ?? false);
    console.log("[TEMP-DEBUG] getInitData() contains '%2B':", initData?.includes("%2B") ?? false);
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
      console.log("[TEMP-DEBUG] Calling linkTelegram()");
      linkTelegram(initData)
        .then(() => console.log("[TEMP-DEBUG] linkTelegram() resolved"))
        .catch((e) => console.log("[TEMP-DEBUG] linkTelegram() rejected:", e));
    } else {
      console.info("[Telegram] Attempting silent Telegram login.");
      console.log("[TEMP-DEBUG] Calling loginWithTelegram()");
      loginWithTelegram(initData)
        .then(() => console.log("[TEMP-DEBUG] loginWithTelegram() resolved — token should now be set"))
        .catch((e) => console.log("[TEMP-DEBUG] loginWithTelegram() rejected:", e));
    }
  }, [token, loading, loginWithTelegram, linkTelegram]);

  return <>{children}</>;
}
