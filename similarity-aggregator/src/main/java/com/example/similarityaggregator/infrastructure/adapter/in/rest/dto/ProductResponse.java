package com.example.similarityaggregator.infrastructure.adapter.in.rest.dto;

import com.example.similarityaggregator.domain.model.Product;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String name,
        BigDecimal price,
        boolean availability
) {
    public static ProductResponse fromDomain(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.price(),
                product.availability()
        );
    }
}