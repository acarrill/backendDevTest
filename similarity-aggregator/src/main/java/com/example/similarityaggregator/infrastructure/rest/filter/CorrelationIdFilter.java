package com.example.similarityaggregator.infrastructure.rest.filter;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = getOrGenerateCorrelationId(exchange.getRequest());

        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        return Mono.fromRunnable(() -> MDC.put(CORRELATION_ID_KEY, correlationId))
                .then(chain.filter(exchange))
                .contextWrite(ctx -> ctx.put(CORRELATION_ID_KEY, correlationId))
                .doFinally(signal -> MDC.remove(CORRELATION_ID_KEY));
    }

    private String getOrGenerateCorrelationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        return correlationId != null && !correlationId.isBlank()
                ? correlationId
                : UUID.randomUUID().toString().substring(0, 8);
    }
}