import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";

export function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <div className="flex min-h-[60vh] items-center py-16">
      <Container className="max-w-lg">
        <p className="mb-2 text-[0.7rem] font-semibold uppercase tracking-[0.2em] text-[--color-muted]">
          {t("notFound.code")}
        </p>
        <h1 className="font-display mb-4 text-4xl font-extrabold uppercase tracking-[-0.01em] text-black md:text-5xl">
          {t("notFound.title")}
        </h1>
        <p className="mb-10 text-[15px] text-[--color-muted]">
          {t("notFound.description")}
        </p>
        <div className="flex flex-wrap gap-3">
          <Link
            to="/"
            className="inline-flex items-center justify-center bg-black px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-white transition hover:bg-zinc-800"
          >
            {t("notFound.toHome")}
          </Link>
          <Link
            to="/catalog"
            className="inline-flex items-center justify-center border border-[--color-border] bg-white px-6 py-3 text-[12px] font-bold uppercase tracking-[0.14em] text-black transition hover:border-black/60"
          >
            {t("notFound.toCatalog")}
          </Link>
        </div>
      </Container>
    </div>
  );
}
