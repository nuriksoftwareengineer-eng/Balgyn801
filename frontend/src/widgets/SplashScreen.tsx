import { useEffect, useState } from "react";

const SPLASH_KEY = "balgyn_splash_seen";
// Build-time flag (frontend/Dockerfile ARG, wired in docker-compose.prod.yml).
const SPLASH_ENABLED = import.meta.env.VITE_ENABLE_SPLASH !== "false";

// Floor, not a fixed hold: on a fast load the splash pads out to this many ms so the
// entrance animation has time to register, but never adds a wall-clock delay beyond it
// (the old version held for a flat 2.4s regardless of load speed — that's gone).
const MIN_VISIBLE_MS = 650;
// Fade-out transition length, applied after the floor/readiness wait, not counted
// against it.
const FADE_OUT_MS = 600;

export function SplashScreen() {
  const [visible, setVisible] = useState(false);
  const [exiting, setExiting] = useState(false);

  useEffect(() => {
    if (!SPLASH_ENABLED) return;
    if (!sessionStorage.getItem(SPLASH_KEY)) {
      setVisible(true);
      sessionStorage.setItem(SPLASH_KEY, "1");
    }
  }, []);

  function dismiss() {
    setExiting(true);
    setTimeout(() => setVisible(false), FADE_OUT_MS);
  }

  useEffect(() => {
    if (!visible) return;
    // "App ready" = this effect running: React has completed its first commit, so the
    // matched route's real content already exists in the DOM underneath this overlay.
    // performance.now() here is elapsed time since navigation start (it's a clock
    // anchored to timeOrigin, not to when this line runs), so it correctly reflects a
    // slow JS/network load, not just React's own near-instant mount cost. Fast load ->
    // pad up to the floor; slow load -> dismiss immediately, no added wait.
    const elapsed = performance.now();
    const remaining = Math.max(0, MIN_VISIBLE_MS - elapsed);
    const id = setTimeout(dismiss, remaining);
    return () => clearTimeout(id);
  }, [visible]);

  if (!visible) return null;

  return (
    <div
      aria-hidden="true"
      style={{ animation: exiting ? "splashFadeOut 0.6s ease forwards" : undefined }}
      className="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-black select-none"
    >
      <style>{`
        @keyframes splashLetterIn {
          from { opacity: 0; transform: translateY(20px) scaleY(0.8); }
          to   { opacity: 1; transform: translateY(0) scaleY(1); }
        }
        @keyframes splashFadeOut {
          from { opacity: 1; }
          to   { opacity: 0; }
        }
        @keyframes splashLineGrow {
          from { width: 0; }
          to   { width: 100%; }
        }
        .splash-letter {
          display: inline-block;
          animation: splashLetterIn 0.5s cubic-bezier(0.16,1,0.3,1) both;
        }
      `}</style>

      <p
        className="text-[clamp(3.5rem,14vw,9rem)] font-bold uppercase leading-none tracking-[-0.04em] text-white"
        style={{ letterSpacing: "-0.04em" }}
      >
        {"BALGYN".split("").map((ch, i) => (
          <span
            key={i}
            className="splash-letter"
            style={{ animationDelay: `${i * 60}ms` }}
          >
            {ch}
          </span>
        ))}
      </p>

      <div className="mt-6 h-px w-32 overflow-hidden bg-white/20">
        <div
          className="h-full bg-white"
          style={{
            animation: "splashLineGrow 1.8s cubic-bezier(0.4,0,0.2,1) 0.5s both",
          }}
        />
      </div>

      <p
        className="mt-4 text-[0.6rem] uppercase tracking-[0.3em] text-white/40"
        style={{ animation: "splashLetterIn 0.6s ease 1s both" }}
      >
        embroidery studio
      </p>
    </div>
  );
}
