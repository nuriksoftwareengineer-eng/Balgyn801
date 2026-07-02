import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { Providers } from "@/app/providers";
import { AppRouter } from "@/app/router";
import { SplashScreen } from "@/widgets/SplashScreen";
import "@/app/i18n";
import "./index.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Providers>
      <SplashScreen />
      <AppRouter />
    </Providers>
  </StrictMode>,
);
