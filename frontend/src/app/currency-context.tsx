import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";

export type Currency = "KZT" | "USD" | "EUR" | "RUB";

interface ExchangeRates {
  kztPerUsd: number;
  kztPerEur: number;
  kztPerRub: number;
}

interface CurrencyContextValue {
  currency: Currency;
  setCurrency: (c: Currency) => void;
  /** Convert a KZT amount to the selected currency. */
  convert: (kzt: number) => number;
  /** Format a KZT amount in the selected currency (with symbol). */
  format: (kzt: number) => string;
  symbol: string;
  rates: ExchangeRates;
}

const DEFAULT_RATES: ExchangeRates = {
  kztPerUsd: 480,
  kztPerEur: 530,
  kztPerRub: 5.3,
};

const STORAGE_KEY = "balgyn_currency";

const CURRENCY_META: Record<Currency, { symbol: string; decimals: number; locale: string }> = {
  KZT: { symbol: "₸", decimals: 0, locale: "ru-RU" },
  USD: { symbol: "$", decimals: 2, locale: "en-US" },
  EUR: { symbol: "€", decimals: 2, locale: "de-DE" },
  RUB: { symbol: "₽", decimals: 0, locale: "ru-RU" },
};

/**
 * Freedom Pay compliance: prices must display in KZT (₸) by default for every
 * visitor. Only an explicit user choice (persisted below) may switch currency —
 * never guess from navigator.language, since that previously showed RUB/USD/EUR
 * to visitors before they made any choice.
 */
function detectDefaultCurrency(): Currency {
  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored && ["KZT", "USD", "EUR", "RUB"].includes(stored)) {
    return stored as Currency;
  }
  return "KZT";
}

const CurrencyContext = createContext<CurrencyContextValue | null>(null);

export function CurrencyProvider({ children }: { children: ReactNode }) {
  const [currency, setCurrencyState] = useState<Currency>(detectDefaultCurrency);
  const [rates, setRates] = useState<ExchangeRates>(DEFAULT_RATES);

  // Fetch live rates from backend (non-blocking — falls back to defaults)
  useEffect(() => {
    const baseUrl = (import.meta.env.VITE_API_BASE_URL as string) ?? "/api/v1";
    fetch(`${baseUrl}/exchange-rates`)
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => {
        if (data && typeof data.kztPerUsd === "number") {
          setRates({
            kztPerUsd: data.kztPerUsd,
            kztPerEur: typeof data.kztPerEur === "number" ? data.kztPerEur : DEFAULT_RATES.kztPerEur,
            kztPerRub: typeof data.kztPerRub === "number" ? data.kztPerRub : DEFAULT_RATES.kztPerRub,
          });
        }
      })
      .catch(() => {/* keep defaults */});
  }, []);

  function setCurrency(c: Currency) {
    setCurrencyState(c);
    localStorage.setItem(STORAGE_KEY, c);
  }

  function convert(kzt: number): number {
    switch (currency) {
      case "USD": return kzt / rates.kztPerUsd;
      case "EUR": return kzt / rates.kztPerEur;
      case "RUB": return kzt / rates.kztPerRub;
      default:    return kzt;
    }
  }

  function format(kzt: number): string {
    const amount = convert(kzt);
    const meta = CURRENCY_META[currency];
    const formatted = new Intl.NumberFormat(meta.locale, {
      minimumFractionDigits: meta.decimals,
      maximumFractionDigits: meta.decimals,
    }).format(amount);
    if (currency === "USD") return `$${formatted}`;
    if (currency === "EUR") return `${formatted} €`;
    if (currency === "RUB") return `${formatted} ₽`;
    return `${formatted} ₸`;
  }

  return (
    <CurrencyContext.Provider
      value={{ currency, setCurrency, convert, format, symbol: CURRENCY_META[currency].symbol, rates }}
    >
      {children}
    </CurrencyContext.Provider>
  );
}

export function useCurrency() {
  const ctx = useContext(CurrencyContext);
  if (!ctx) throw new Error("useCurrency must be used inside CurrencyProvider");
  return ctx;
}
