package com.nurba.java.api;

import com.nurba.java.dto.delivery.DeliveryMethodResponse;
import com.nurba.java.dto.delivery.IntlQuoteResponse;
import com.nurba.java.enums.IntlShipKind;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Delivery", description = "Расчёт доставки и справочники")
@RequestMapping("/api/v1/delivery")
public interface DeliveryMethodsApi {

    @Operation(
        summary = "Доступные методы доставки для страны",
        description = "Возвращает список доступных методов доставки с метаданными (метки, ограничения, требования к адресу). "
                    + "Фронтенд отображает только то, что возвращает бэкенд — никакие методы, метки или ограничения "
                    + "не захардкожены на клиенте."
    )
    @GetMapping("/methods")
    List<DeliveryMethodResponse> availableMethods(@RequestParam String countryIso2);

    @Operation(
        summary = "Стоимость международной доставки",
        description = "страна → тарифная зона → цена за тип перевозки (AIR/GROUND). "
                    + "Цены берутся из импортированных таблиц тарифов на бэкенде."
    )
    @GetMapping("/intl-quote")
    IntlQuoteResponse intlQuote(@RequestParam String countryIso2, @RequestParam IntlShipKind kind);
}
