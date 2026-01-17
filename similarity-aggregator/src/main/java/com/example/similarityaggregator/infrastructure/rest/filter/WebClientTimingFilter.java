// infrastructure/filter/WebClientTimingFilter.java
package com.example.similarityaggregator.infrastructure.rest.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class WebClientTimingFilter implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(WebClientTimingFilter.class);

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        long startTime = System.currentTimeMillis();
        String method = request.method().name();
        String url = request.url().toString();

        log.debug("Applying time filter: {} {}", method, url);

        return next.exchange(request)
                .doOnSuccess(response -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("? {} {} - {} ({}ms)", method, url, response.statusCode().value(), elapsed);
                })
                .doOnError(error -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    log.error("✗ {} {} - ({}ms)", method, url, elapsed);
                });
    }
}