import { useTranslation } from "react-i18next";

export function AnnouncementBar() {
  const { t } = useTranslation();

  return (
    <div className="sticky top-0 z-[120] flex h-9 items-center justify-center bg-black px-4">
      <p className="text-center text-[0.65rem] font-medium uppercase tracking-[0.18em] text-white">
        {t("home.announcement")}
      </p>
    </div>
  );
}
