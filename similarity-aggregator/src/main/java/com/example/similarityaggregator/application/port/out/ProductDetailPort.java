package com.example.similarityaggregator.application.port.out;

import com.example.similarityaggregator.domain.model.Product;
import reactor.core.publisher.Mono;

public interface ProductDetailPort {
    Mono<Product> getProductDetail(String productId);
}
