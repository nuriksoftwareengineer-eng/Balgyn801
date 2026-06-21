import { InfoPage, InfoSection } from "@/shared/ui/info-page";

/**
 * Справочная страница о доставке и оплате. Носит описательный характер:
 * фактический список методов и стоимость для конкретного заказа всегда
 * рассчитывает бэкенд на шаге оформления (GET /delivery/methods).
 */
export function DeliveryInfoPage() {
  return (
    <InfoPage
      title="Доставка и оплата"
      lead="Доступные способы доставки зависят от региона и показываются при оформлении заказа. Точная стоимость рассчитывается автоматически на шаге «Доставка»."
    >
      <InfoSection heading="Казахстан">
        <div className="flex flex-col divide-y divide-[--color-border] border border-[--color-border] bg-white">
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">Самовывоз</p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              Бесплатно. Передача заказа в Алматы по договорённости — место и
              время согласуем после оформления.
            </p>
          </div>
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">
              Курьер / такси
            </p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              Доставка по Алматы. Фиксированная стоимость показывается при
              оформлении заказа.
            </p>
          </div>
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">Казпочта</p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              Доставка по всему Казахстану. Стоимость зависит от веса
              отправления.
            </p>
          </div>
          <div className="px-4 py-3">
            <p className="m-0 text-sm font-semibold text-black">СДЭК</p>
            <p className="m-0 mt-0.5 text-xs text-[--color-muted]">
              До пункта выдачи в вашем городе. Стоимость и срок рассчитываются
              по тарифам СДЭК при оформлении.
            </p>
          </div>
        </div>
      </InfoSection>

      <InfoSection heading="Россия и СНГ">
        <p>
          Доставка службой СДЭК до пункта выдачи. На шаге оформления вы
          выбираете город и ПВЗ — стоимость и срок рассчитываются автоматически.
        </p>
      </InfoSection>

      <InfoSection heading="Другие страны">
        <p>
          Международная доставка почтовым отправлением. Стоимость зависит от
          страны и веса; сроки — от 14 дней. Детали согласуем при подтверждении
          заказа.
        </p>
      </InfoSection>

      <InfoSection heading="Оплата">
        <p>
          После оформления заказа доступна оплата через Freedom Pay (карты
          казахстанских банков) или PayPal (международные карты). Заказ
          передаётся в производство после подтверждения оплаты.
        </p>
        <p>
          Цены на сайте указаны в тенге (₸). Реквизиты карт обрабатываются
          платёжным провайдером — мы их не получаем и не храним.
        </p>
      </InfoSection>

      <InfoSection heading="Сроки">
        <p>
          Изделия вышиваются под заказ: срок изготовления сообщается при
          подтверждении и обычно составляет несколько рабочих дней. Перед
          отправкой присылаем фото готового изделия.
        </p>
      </InfoSection>
    </InfoPage>
  );
}
