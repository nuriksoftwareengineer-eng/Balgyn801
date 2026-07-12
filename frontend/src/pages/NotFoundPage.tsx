import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Container } from "@/shared/ui/container";

export function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <div className="flex min-h-[70vh] items-center py-20">
      <Container>
        <div className="mx-auto flex max-w-xl flex-col items-center text-center">
          <p className="mb-4 text-[11px] font-medium uppercase tracking-[0.24em] text-[--color-muted]">
            {t("notFound.code")}
          </p>
          <h1 className="display text-[44px] uppercase text-black md:text-[64px]">
            {t("notFound.title")}
          </h1>
          <p className="mt-5 max-w-md text-[15px] leading-relaxed text-[--color-muted]">
            {t("notFound.description")}
          </p>
          <div className="mt-10 flex flex-wrap justify-center gap-3">
            <Link
              to="/"
              className="inline-flex items-center justify-center bg-black px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-white transition hover:bg-zinc-800"
            >
              {t("notFound.toHome")}
            </Link>
            <Link
              to="/catalog"
              className="inline-flex items-center justify-center border border-[--color-border] bg-white px-7 py-3.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-black transition hover:border-black"
            >
              {t("notFound.toCatalog")}
            </Link>
          </div>
        </div>
      </Container>
    </div>
  );
}
