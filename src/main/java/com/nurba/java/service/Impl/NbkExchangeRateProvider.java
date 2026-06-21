package com.nurba.java.service.Impl;

import com.nurba.java.service.ExchangeRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches the KZT-per-USD rate from the National Bank of Kazakhstan RSS feed.
 * <p>
 * Fully defensive: a short timeout and a catch-all mean any failure yields {@link Optional#empty()},
 * leaving the cached rate untouched. This source is consulted only by the scheduled updater, so a
 * failure here never affects checkout.
 */
@Component
public class NbkExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(NbkExchangeRateProvider.class);

    private static final Pattern USD_RATE = Pattern.compile(
            "<title>USD</title>.*?<description>\\s*([0-9]+(?:[.,][0-9]+)?)\\s*</description>",
            Pattern.DOTALL);
    private static final Pattern EUR_RATE = Pattern.compile(
            "<title>EUR</title>.*?<description>\\s*([0-9]+(?:[.,][0-9]+)?)\\s*</description>",
            Pattern.DOTALL);
    private static final Pattern RUB_RATE = Pattern.compile(
            "<title>RUB</title>.*?<description>\\s*([0-9]+(?:[.,][0-9]+)?)\\s*</description>",
            Pattern.DOTALL);

    @Value("${app.exchange-rate.nbk-rss-url:https://nationalbank.kz/rss/rates_all.xml}")
    private String rssUrl;

    @Value("${app.exchange-rate.fetch-timeout-ms:3000}")
    private long timeoutMs;

    @Override
    public Optional<BigDecimal> fetchKztPerUsd() {
        return fetchFromFeed(USD_RATE, "USD");
    }

    @Override
    public Optional<BigDecimal> fetchKztPerEur() {
        return fetchFromFeed(EUR_RATE, "EUR");
    }

    @Override
    public Optional<BigDecimal> fetchKztPerRub() {
        return fetchFromFeed(RUB_RATE, "RUB");
    }

    private Optional<BigDecimal> fetchFromFeed(Pattern pattern, String currency) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(rssUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null) {
                log.warn("NBK rate fetch returned status {} for {}", response.statusCode(), currency);
                return Optional.empty();
            }
            Matcher m = pattern.matcher(response.body());
            if (!m.find()) {
                log.warn("NBK rate fetch: {} entry not found in feed", currency);
                return Optional.empty();
            }
            BigDecimal rate = new BigDecimal(m.group(1).replace(',', '.'));
            if (rate.signum() <= 0) {
                return Optional.empty();
            }
            return Optional.of(rate);
        } catch (Exception e) {
            log.warn("NBK rate fetch failed for {} ({}); keeping last known-good rate", currency, e.toString());
            return Optional.empty();
        }
    }
}
