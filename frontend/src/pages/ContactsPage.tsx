import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { ArrowUpRight } from "lucide-react";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
  TELEGRAM_CHANNEL_URL,
  WHATSAPP_URL,
} from "@/shared/constants/store-content";
import { Container } from "@/shared/ui/container";
import { MerchantDetails } from "@/shared/ui/MerchantDetails";

interface Channel {
  label: string;
  value: string;
  href: string;
  external?: boolean;
}

const CHANNELS: Channel[] = [
  { label: "Telegram", value: "@balgyn.bol", href: STORE_TELEGRAM_URL, external: true },
  { label: "Instagram", value: "@balgyn.bol", href: "https://instagram.com/balgyn.bol", external: true },
  { label: "WhatsApp", value: "+7 708 193 75 10", href: WHATSAPP_URL, external: true },
  { label: "Email", value: CONTACT_EMAIL, href: `mailto:${CONTACT_EMAIL}` },
  { label: "Telegram Channel", value: "@balgyncatalog", href: TELEGRAM_CHANNEL_URL, external: true },
];

function ChannelBlock({ ch }: { ch: Channel }) {
  const ext = ch.external ? { target: "_blank", rel: "noopener noreferrer" } : {};
  return (
    <a
      href={ch.href}
      {...ext}
      className="group relative flex flex-col justify-between gap-10 bg-white p-6 transition-colors duration-500 hover:bg-black md:p-8"
    >
      <span className="text-[10px] uppercase tracking-[0.22em] text-[--color-muted] transition-colors group-hover:text-white/60">
        {ch.label}
      </span>
      <span className="flex items-end justify-between gap-3">
        <span className="text-[18px] font-medium tracking-[-0.01em] text-black transition-colors group-hover:text-white md:text-[22px]">
          {ch.value}
        </span>
        <ArrowUpRight
          className="h-5 w-5 shrink-0 text-black transition-all duration-500 group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-white"
          strokeWidth={1.5}
        />
      </span>
    </a>
  );
}

export function ContactsPage() {
  const { t } = useTranslation();

  return (
    <div className="py-12 md:py-20">
      <Container>
        {/* Header */}
        <nav className="mb-6 flex items-center gap-2 text-[10px] uppercase tracking-[0.16em] text-[--color-muted]">
          <Link to="/" className="transition-colors hover:text-black">{t("nav.home")}</Link>
          <span aria-hidden>/</span>
          <span className="text-black">{t("contactsPage.title")}</span>
        </nav>

        <div className="max-w-2xl">
          <h1 className="display text-[44px] uppercase text-black md:text-[64px]">
            {t("contactsPage.title")}
          </h1>
          <p className="mt-5 text-[15px] leading-relaxed text-[--color-muted]">
            {t("contactsPage.lead")}
          </p>
        </div>

        {/* Channel grid — hairline-divided blocks */}
        <div className="mt-12 grid gap-px border border-[--color-border] bg-[--color-border] sm:grid-cols-2 lg:grid-cols-3">
          {CHANNELS.map((ch) => (
            <ChannelBlock key={ch.label} ch={ch} />
          ))}
          {/* Filler cell to keep the grid clean at lg (6th slot) */}
          <div className="hidden bg-white lg:block" aria-hidden />
        </div>

        {/* Secondary info */}
        <div className="mt-16 grid gap-10 border-t border-[--color-border] pt-12 md:grid-cols-2">
          <div>
            <p className="mb-3 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
              {t("contactsPage.pickupHeading")}
            </p>
            <p className="text-[14px] leading-relaxed text-zinc-700">{t("contactsPage.pickupDesc")}</p>
          </div>
          <div>
            <p className="mb-3 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
              {t("contactsPage.ordersHeading")}
            </p>
            <p className="text-[14px] leading-relaxed text-zinc-700">{t("contactsPage.ordersDesc")}</p>
          </div>
        </div>

        {/* Merchant details — compliance */}
        <div className="mt-16 border-t border-[--color-border] pt-12">
          <p className="mb-4 text-[10px] uppercase tracking-[0.2em] text-[--color-muted]">
            {t("merchant.title")}
          </p>
          <MerchantDetails />
        </div>
      </Container>
    </div>
  );
}
