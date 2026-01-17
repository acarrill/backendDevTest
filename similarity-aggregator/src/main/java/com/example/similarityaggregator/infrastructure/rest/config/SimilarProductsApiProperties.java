package com.example.similarityaggregator.infrastructure.rest.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "similar-products.api")
public record SimilarProductsApiProperties(
        @NotBlank String baseUrl,
        @NotNull Duration timeout
) {}