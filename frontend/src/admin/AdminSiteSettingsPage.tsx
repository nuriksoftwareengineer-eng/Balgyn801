import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/app/auth-context";
import { uploadMedia } from "@/shared/api/backend-api";
import { apiFetch } from "@/shared/api/http";
import { Button } from "@/shared/ui/button";

function useSiteSettingAdmin(key: string) {
  const { token } = useAuth();
  return useQuery({
    queryKey: ["admin-site-setting", key],
    queryFn: () =>
      apiFetch<{ key: string; value: string | null }>(`/admin/site-settings/${key}`, { token: token! }),
    enabled: !!token,
  });
}

function useSetSiteSetting() {
  const { token } = useAuth();
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ key, value }: { key: string; value: string | null }) =>
      apiFetch(`/admin/site-settings/${key}`, {
        method: "PUT",
        body: JSON.stringify({ value }),
        token: token!,
      }),
    onSuccess: (_, { key }) => {
      void qc.invalidateQueries({ queryKey: ["admin-site-setting", key] });
      void qc.invalidateQueries({ queryKey: ["site-settings"] });
    },
  });
}

/** One admin-editable multi-line text setting — Production/Delivery/Shipping/Care
 *  Instructions all follow this exact shape, reusing the same generic get/set hooks
 *  the CEO-photo section above uses. */
function TextSettingSection({
  settingKey,
  label,
  description,
  placeholder,
}: {
  settingKey: string;
  label: string;
  description: string;
  placeholder: string;
}) {
  const q = useSiteSettingAdmin(settingKey);
  const setSetting = useSetSiteSetting();
  const [value, setValue] = useState<string | null>(null);
  const [saveMsg, setSaveMsg] = useState<string | null>(null);

  const current = value ?? q.data?.value ?? "";

  async function handleSave() {
    setSaveMsg(null);
    await setSetting.mutateAsync({ key: settingKey, value: current.trim() || null });
    setSaveMsg("Сохранено");
    setTimeout(() => setSaveMsg(null), 3000);
  }

  return (
    <section className="mt-6 rounded-[14px] border border-white/10 bg-zinc-900/50 p-6">
      <h2 className="mb-1 text-sm font-semibold uppercase tracking-wider text-zinc-400">
        {label}
      </h2>
      <p className="mb-5 text-xs text-zinc-500">{description}</p>

      {q.isLoading ? (
        <div className="mb-4 h-28 animate-pulse bg-zinc-800" />
      ) : (
        <textarea
          value={current}
          onChange={(e) => setValue(e.target.value)}
          placeholder={placeholder}
          rows={5}
          className="mb-4 w-full rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40"
        />
      )}

      {saveMsg && <p className="mb-3 text-xs text-emerald-400">{saveMsg}</p>}

      <Button type="button" disabled={setSetting.isPending} onClick={handleSave}>
        {setSetting.isPending ? "Сохранение…" : "Сохранить"}
      </Button>
    </section>
  );
}

export function AdminSiteSettingsPage() {
  const { token } = useAuth();
  const ceoQ = useSiteSettingAdmin("ceo_photo_url");
  const setSetting = useSetSiteSetting();

  const [urlInput, setUrlInput] = useState("");
  const [uploading, setUploading] = useState(false);
  const [uploadErr, setUploadErr] = useState<string | null>(null);
  const [saveMsg, setSaveMsg] = useState<string | null>(null);

  const currentUrl = ceoQ.data?.value ?? null;

  async function handleFileUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file || !token) return;
    setUploading(true);
    setUploadErr(null);
    try {
      const res = await uploadMedia(file, token);
      setUrlInput(res.publicUrl);
    } catch (err) {
      setUploadErr(err instanceof Error ? err.message : "Ошибка загрузки");
    } finally {
      setUploading(false);
    }
  }

  async function handleSave() {
    setSaveMsg(null);
    const val = urlInput.trim() || null;
    await setSetting.mutateAsync({ key: "ceo_photo_url", value: val });
    setSaveMsg("Сохранено");
    setTimeout(() => setSaveMsg(null), 3000);
  }

  async function handleClear() {
    setSaveMsg(null);
    setUrlInput("");
    await setSetting.mutateAsync({ key: "ceo_photo_url", value: null });
    setSaveMsg("Фото удалено");
    setTimeout(() => setSaveMsg(null), 3000);
  }

  return (
    <div className="max-w-lg">
      <h1 className="mb-1 text-2xl font-bold text-zinc-100">Настройки сайта</h1>
      <p className="mb-8 text-sm text-zinc-500">
        Управление публичными настройками страницы «О нас» и других разделов.
      </p>

      {/* CEO Photo */}
      <section className="rounded-[14px] border border-white/10 bg-zinc-900/50 p-6">
        <h2 className="mb-1 text-sm font-semibold uppercase tracking-wider text-zinc-400">
          Фото CEO
        </h2>
        <p className="mb-5 text-xs text-zinc-500">
          Отображается на странице «О нас». Если не задано — показывается заглушка с инициалами.
        </p>

        {/* Current preview */}
        {ceoQ.isLoading ? (
          <div className="mb-4 h-40 w-32 animate-pulse bg-zinc-800" />
        ) : currentUrl ? (
          <div className="mb-4">
            <p className="mb-1.5 text-xs text-zinc-500">Текущее фото</p>
            <img
              src={currentUrl}
              alt="CEO"
              className="h-40 w-32 rounded-none border border-white/10 object-cover object-center"
            />
          </div>
        ) : (
          <div className="mb-4 flex h-40 w-32 items-center justify-center border border-white/10 bg-zinc-800 text-sm text-zinc-500">
            Нет фото
          </div>
        )}

        {/* Upload */}
        <label className="mb-4 flex flex-col gap-1.5">
          <span className="text-xs text-zinc-400">Загрузить из файла</span>
          <input
            type="file"
            accept="image/*"
            disabled={uploading}
            onChange={handleFileUpload}
            className="text-sm text-zinc-300 file:mr-3 file:rounded file:border-0 file:bg-zinc-700 file:px-3 file:py-1 file:text-xs file:font-semibold file:text-zinc-200 hover:file:bg-zinc-600 disabled:opacity-50"
          />
        </label>

        {/* URL input */}
        <label className="mb-4 flex flex-col gap-1.5">
          <span className="text-xs text-zinc-400">Или вставить URL напрямую</span>
          <input
            value={urlInput}
            onChange={(e) => setUrlInput(e.target.value)}
            placeholder="https://cdn.example.com/photo.jpg"
            className="rounded border border-white/20 bg-zinc-800 px-3 py-2 text-sm text-zinc-100 placeholder:text-zinc-600 outline-none focus:border-white/40"
          />
        </label>

        {/* Preview of new URL */}
        {urlInput && (
          <div className="mb-4">
            <p className="mb-1 text-xs text-zinc-500">Предпросмотр</p>
            <img
              src={urlInput}
              alt="preview"
              className="h-40 w-32 rounded-none border border-white/10 object-cover object-center"
            />
          </div>
        )}

        {uploadErr && (
          <p className="mb-3 text-xs text-red-400">{uploadErr}</p>
        )}
        {saveMsg && (
          <p className="mb-3 text-xs text-emerald-400">{saveMsg}</p>
        )}

        <div className="flex gap-3">
          <Button
            type="button"
            disabled={uploading || setSetting.isPending || !urlInput.trim()}
            onClick={handleSave}
          >
            {setSetting.isPending ? "Сохранение…" : "Сохранить"}
          </Button>
          {currentUrl && (
            <button
              type="button"
              disabled={setSetting.isPending}
              onClick={handleClear}
              className="text-sm font-semibold text-red-400 hover:text-red-300 disabled:opacity-40"
            >
              Удалить фото
            </button>
          )}
        </div>
      </section>

      {/* Product-page info block + accordion — shown on every design/product page */}
      <TextSettingSection
        settingKey="production_description"
        label="Production"
        description="Короткий текст в карточке «Production» на странице товара."
        placeholder={"Handmade after order.\nProduction takes 5–10 business days."}
      />
      <TextSettingSection
        settingKey="delivery_description"
        label="Delivery"
        description="Короткий текст в карточке «Delivery» на странице товара."
        placeholder={"Worldwide delivery.\nDelivery time depends on destination country."}
      />
      <TextSettingSection
        settingKey="shipping_description"
        label="Shipping (аккордеон)"
        description="Подробный текст в разделе «Shipping» аккордеона на странице товара — отдельно от короткого текста карточки Delivery выше."
        placeholder={"Orders are produced within 7–10 business days.\nDelivery time:\nEurope — 2–3 weeks.\nUSA — 2–4 weeks.\nAsia — 3–5 weeks."}
      />
      <TextSettingSection
        settingKey="care_instructions"
        label="Care Instructions"
        description="Текст в разделе «Care Instructions» аккордеона на странице товара."
        placeholder="Machine wash cold. Do not bleach. Tumble dry low."
      />
    </div>
  );
}
