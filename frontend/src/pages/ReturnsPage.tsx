import { Link } from "react-router-dom";
import {
  CONTACT_EMAIL,
  STORE_TELEGRAM_URL,
} from "@/shared/constants/store-content";
import { InfoPage, InfoSection } from "@/shared/ui/info-page";

const linkClass =
  "font-medium text-black underline underline-offset-2 transition-colors hover:text-zinc-600";

export function ReturnsPage() {
  return (
    <InfoPage
      title="Возврат и обмен"
      lead="Мы проверяем каждое изделие перед отправкой и присылаем фото. Если что-то всё же пошло не так — вот как мы решаем такие ситуации."
    >
      <InfoSection heading="Товары из каталога">
        <p>
          Изделие из каталога надлежащего качества можно обменять или вернуть в
          течение 14 дней с момента получения, если оно не было в носке,
          сохранены ярлыки и товарный вид (ст. 30 Закона РК «О защите прав
          потребителей»).
        </p>
        <p>
          Стоимость обратной пересылки при возврате товара надлежащего качества
          оплачивает покупатель.
        </p>
      </InfoSection>

      <InfoSection heading="Индивидуальные заказы">
        <p>
          Изделия, изготовленные по вашему макету через раздел{" "}
          <Link to="/custom-design" className={linkClass}>
            «Свой дизайн»
          </Link>
          , являются товаром с индивидуально-определёнными свойствами и
          возврату/обмену не подлежат — кроме случаев производственного брака.
        </p>
      </InfoSection>

      <InfoSection heading="Брак или ошибка в заказе">
        <p>
          Если изделие пришло с дефектом вышивки, не тем размером или цветом —
          напишите нам в течение 7 дней после получения: пришлите номер заказа и
          фото проблемы. Мы бесплатно заменим изделие или вернём деньги — на ваш
          выбор. Обратную пересылку в этом случае оплачиваем мы.
        </p>
      </InfoSection>

      <InfoSection heading="Как оформить возврат">
        <ol className="list-decimal space-y-1.5 pl-5">
          <li>
            Свяжитесь с нами:{" "}
            <a href={`mailto:${CONTACT_EMAIL}`} className={linkClass}>
              {CONTACT_EMAIL}
            </a>{" "}
            или{" "}
            <a
              href={STORE_TELEGRAM_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={linkClass}
            >
              Telegram
            </a>
            . Укажите номер заказа и причину.
          </li>
          <li>Мы подтвердим возврат и согласуем способ обратной отправки.</li>
          <li>
            Деньги возвращаются тем же способом, которым была произведена
            оплата, в течение 10 рабочих дней после получения изделия обратно.
          </li>
        </ol>
      </InfoSection>
    </InfoPage>
  );
}
