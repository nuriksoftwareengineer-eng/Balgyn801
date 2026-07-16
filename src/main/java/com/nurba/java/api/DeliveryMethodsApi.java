package com.nurba.java.api;

import com.nurba.java.dto.delivery.DeliveryMethodResponse;
import com.nurba.java.dto.delivery.IntlQuoteRequest;
import com.nurba.java.dto.delivery.IntlQuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
        description = "страна → тарифная зона → официальный тариф Казпочты по весу и типу перевозки "
                    + "(AIR/GROUND). Вес считается на бэкенде из переданных позиций корзины — "
                    + "клиент вес не передаёт. POST, так как тело содержит список позиций."
    )
    @PostMapping("/intl-quote")
    IntlQuoteResponse intlQuote(@Valid @RequestBody IntlQuoteRequest request);
}
