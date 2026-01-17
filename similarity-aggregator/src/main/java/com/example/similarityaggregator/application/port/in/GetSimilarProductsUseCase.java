package com.example.similarityaggregator.application.port.in;

import com.example.similarityaggregator.domain.model.Product;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetSimilarProductsUseCase {

    Mono<List<Product>> getSimilarProducts(String productId);
}