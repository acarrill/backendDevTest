package com.example.similarityaggregator.infrastructure.rest.adapter.in;


import com.example.similarityaggregator.application.port.in.GetSimilarProductsUseCase;
import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.domain.model.Product;
import com.example.similarityaggregator.infrastructure.rest.adapter.in.SimilarProductsController;
import com.example.similarityaggregator.infrastructure.rest.adapter.in.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;

@WebFluxTest(SimilarProductsController.class)
class SimilarProductsControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GetSimilarProductsUseCase getSimilarProductsUseCase;

    @Test
    @DisplayName("Should return similar products")
    void shouldReturnSimilarProducts() {
        // Given
        String productId = "1";
        List<Product> products = List.of(
                new Product("2", "Product 2", new BigDecimal("19.99"), true)
        );

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(Mono.just(products));

        // When & Then
        webTestClient.get()
                .uri("/product/{productId}/similar", productId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductResponse.class)
                .hasSize(1)
                .contains(new ProductResponse("2", "Product 2", new BigDecimal("19.99"), true));
    }

    @Test
    @DisplayName("Should return 404 if product not found")
    void shouldReturnNotFound() {
        // Given
        String productId = "1";

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(Mono.error(new ProductNotFoundException(productId)));

        // When & Then
        webTestClient.get()
                .uri("/product/{productId}/similar", productId)
                .exchange()
                .expectStatus().isNotFound();
    }
}