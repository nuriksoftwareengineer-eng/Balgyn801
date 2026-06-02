package com.nurba.java.api;

import com.nurba.java.dto.delivery.CdekCityDto;
import com.nurba.java.dto.delivery.CdekDeliveryPointDto;
import com.nurba.java.dto.delivery.CdekOrderTariffRequest;
import com.nurba.java.dto.delivery.CdekOrderTariffResponse;
import com.nurba.java.dto.delivery.CdekTariffRequest;
import com.nurba.java.dto.delivery.CdekTariffResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Публичные эндпойнты для оформления заказа на витрине.
 * Пока поддерживается только СДЭК; при росте провайдеров путь поменяем на {@code /delivery/{provider}/...}.
 */
@Tag(name = "Delivery", description = "Расчёт доставки и справочники (СДЭК)")
@RequestMapping("/api/v1/delivery/cdek")
public interface DeliveryApi {

    @Operation(summary = "Поиск городов СДЭК по подстроке (для автодополнения)")
    @GetMapping("/cities")
    List<CdekCityDto> searchCities(
            @RequestParam(name = "q") String query,
            @RequestParam(name = "limit", required = false) Integer limit
    );

    @Operation(summary = "ПВЗ/постаматы в городе (по коду города из справочника)")
    @GetMapping("/points")
    List<CdekDeliveryPointDto> deliveryPoints(
            @RequestParam(name = "cityCode") int cityCode
    );

    @Operation(summary = "Расчёт стоимости и срока доставки")
    @PostMapping("/calculate")
    CdekTariffResponse calculate(@Valid @RequestBody CdekTariffRequest request);

    @Operation(summary = "Расчёт доставки СДЭК по корзине (сумма товаров + итог заказа)")
    @PostMapping("/calculate-order")
    CdekOrderTariffResponse calculateOrder(@Valid @RequestBody CdekOrderTariffRequest request);
}
