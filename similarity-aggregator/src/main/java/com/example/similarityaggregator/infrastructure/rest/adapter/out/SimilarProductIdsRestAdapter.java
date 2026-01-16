package com.example.similarityaggregator.infrastructure.rest.adapter.out;

import com.example.similarityaggregator.application.port.out.SimilarProductIdsPort;
import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.infrastructure.rest.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class SimilarProductIdsRestAdapter implements SimilarProductIdsPort {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductIdsRestAdapter.class);

    private final WebClient webClient;

    public SimilarProductIdsRestAdapter(WebClient.Builder webClientBuilder,
                                        @Value("${similar-products.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    @Cacheable(value = "similar-ids", key = "#productId")
    @CircuitBreaker(name = "similarIds", fallbackMethod = "fallbackSimilarIds")
    public Mono<List<String>> getSimilarIds(String productId) {
        log.info("Fetching similar ids for productId={}", productId);

        return webClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .bodyToMono(String[].class)
                .map(List::of)
                .doOnNext(ids -> log.info("Found {} similar ids for productId={}", ids.size(), productId))
                .onErrorResume(WebClientResponseException.NotFound.class,
                        e -> Mono.error(new ProductNotFoundException(productId)));
    }

    public Mono<List<String>> fallbackSimilarIds(String productId, Throwable t) {
        log.error("Circuit breaker fallback similarIds, productId={}", productId, t);
        return Mono.error(t);
    }
}