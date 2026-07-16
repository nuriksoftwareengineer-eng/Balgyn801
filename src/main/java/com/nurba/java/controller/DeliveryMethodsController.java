package com.nurba.java.controller;

import com.nurba.java.api.DeliveryMethodsApi;
import com.nurba.java.dto.delivery.DeliveryMethodResponse;
import com.nurba.java.dto.delivery.IntlQuoteResponse;
import com.nurba.java.enums.IntlShipKind;
import com.nurba.java.service.delivery.DeliveryPricingService;
import com.nurba.java.service.delivery.InternationalShippingQuote;
import com.nurba.java.service.delivery.InternationalShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeliveryMethodsController implements DeliveryMethodsApi {

    private final DeliveryPricingService deliveryPricingService;
    private final InternationalShippingService internationalShippingService;

    @Override
    public List<DeliveryMethodResponse> availableMethods(String countryIso2) {
        return deliveryPricingService.availableMethods(countryIso2);
    }

    @Override
    public IntlQuoteResponse intlQuote(String countryIso2, IntlShipKind kind) {
        InternationalShippingQuote quote = internationalShippingService.quote(countryIso2, kind);
        return new IntlQuoteResponse(quote.feeKzt(), quote.feeUsd());
    }
}
