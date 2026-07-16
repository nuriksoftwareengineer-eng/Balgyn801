package com.nurba.java.controller;

import com.nurba.java.api.DeliveryMethodsApi;
import com.nurba.java.dto.delivery.DeliveryMethodResponse;
import com.nurba.java.dto.delivery.IntlQuoteRequest;
import com.nurba.java.dto.delivery.IntlQuoteResponse;
import com.nurba.java.service.GarmentWeightService;
import com.nurba.java.service.delivery.DeliveryPricingService;
import com.nurba.java.service.delivery.InternationalShippingQuote;
import com.nurba.java.service.delivery.InternationalShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class DeliveryMethodsController implements DeliveryMethodsApi {

    private final DeliveryPricingService deliveryPricingService;
    private final InternationalShippingService internationalShippingService;
    private final GarmentWeightService garmentWeightService;

    @Override
    public List<DeliveryMethodResponse> availableMethods(String countryIso2) {
        return deliveryPricingService.availableMethods(countryIso2);
    }

    @Override
    public IntlQuoteResponse intlQuote(IntlQuoteRequest request) {
        Map<Long, Integer> qtyByGarment = request.items() == null
                ? Map.of()
                : request.items().stream().collect(Collectors.toMap(
                        IntlQuoteRequest.Item::designGarmentId,
                        IntlQuoteRequest.Item::quantity,
                        Integer::sum));
        BigDecimal weightKg = garmentWeightService.calculateWeightForDesignGarments(qtyByGarment);
        InternationalShippingQuote quote =
                internationalShippingService.quote(request.countryIso2(), request.kind(), weightKg);
        return new IntlQuoteResponse(quote.feeKzt(), quote.feeUsd());
    }
}
