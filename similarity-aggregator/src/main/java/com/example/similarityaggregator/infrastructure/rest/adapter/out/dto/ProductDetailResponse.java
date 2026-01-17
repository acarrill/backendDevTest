package com.example.similarityaggregator.infrastructure.rest.adapter.out.dto;

import com.example.similarityaggregator.domain.model.Product;

import java.math.BigDecimal;

public record ProductDetailResponse(
        String id,
        String name,
        BigDecimal price,
        boolean availability
) {
    public Product toDomain() {
        return new Product(id, name, price, availability);
    }
}