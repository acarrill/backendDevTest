package com.example.similarityaggregator.infrastructure.rest.config;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class ReactorContextConfig {

    private static final String CORRELATION_ID_KEY = "correlationId";

    @PostConstruct
    public void setupContextPropagation() {
        Hooks.enableAutomaticContextPropagation();

        ContextRegistry.getInstance().registerThreadLocalAccessor(
                CORRELATION_ID_KEY,
                () -> MDC.get(CORRELATION_ID_KEY),
                value -> MDC.put(CORRELATION_ID_KEY, value),
                () -> MDC.remove(CORRELATION_ID_KEY)
        );
    }
}