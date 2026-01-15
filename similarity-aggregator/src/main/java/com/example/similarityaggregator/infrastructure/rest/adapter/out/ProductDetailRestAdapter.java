package com.example.similarityaggregator.infrastructure.rest.adapter.out;

import com.example.similarityaggregator.application.port.out.ProductDetailPort;
import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.domain.model.Product;
import com.example.similarityaggregator.infrastructure.rest.adapter.out.dto.ProductDetailResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class ProductDetailRestAdapter implements ProductDetailPort {

    private static final Logger log = LoggerFactory.getLogger(ProductDetailRestAdapter.class);

    private final WebClient webClient;

    public ProductDetailRestAdapter(WebClient.Builder webClientBuilder,
                                    @Value("${similar-products.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    @Cacheable(value = "product-detail", key = "#productId")
    @CircuitBreaker(name = "productDetail", fallbackMethod = "fallbackProductDetail")
    public Mono<Product> getProductDetail(String productId) {
        log.info("Fetching product detail for productId={}", productId);

        return webClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .bodyToMono(ProductDetailResponse.class)
                .map(ProductDetailResponse::toDomain)
                .doOnNext(product -> log.info("Found product: {}", product.id()))
                .onErrorResume(WebClientResponseException.NotFound.class,
                        e -> Mono.error(new ProductNotFoundException(productId)));
    }

    private Mono<Product> fallbackProductDetail(String productId, Throwable t) {
        log.warn("Circuit breaker fallback for productDetail, productId={}, error={}", productId, t.getMessage());
        return Mono.empty();
    }
}