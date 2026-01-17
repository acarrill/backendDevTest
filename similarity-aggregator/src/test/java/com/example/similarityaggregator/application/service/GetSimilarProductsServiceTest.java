package com.example.similarityaggregator.application.service;

import com.example.similarityaggregator.application.port.out.ProductDetailPort;
import com.example.similarityaggregator.application.port.out.SimilarProductIdsPort;
import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSimilarProductsServiceTest {

    private static final String EXISTING_PRODUCT_ID = "1";
    private static final String NON_EXISTING_PRODUCT_ID = "999";

    @Mock
    private SimilarProductIdsPort similarProductIdsPort;

    @Mock
    private ProductDetailPort productDetailPort;

    private GetSimilarProductsService service;

    @BeforeEach
    void setUp() {
        service = new GetSimilarProductsService(similarProductIdsPort, productDetailPort);
    }

    @Test
    @DisplayName("Should return similar products")
    void shouldReturnSimilarProducts() {
        // Given
        List<String> similarIds = List.of("2", "3");
        Product product2 = new Product("2", "Product 2", new BigDecimal("19.99"), true);
        Product product3 = new Product("3", "Product 3", new BigDecimal("29.99"), false);

        when(similarProductIdsPort.getSimilarIds(EXISTING_PRODUCT_ID))
                .thenReturn(Mono.just(similarIds));
        when(productDetailPort.getProductDetail("2"))
                .thenReturn(Mono.just(product2));
        when(productDetailPort.getProductDetail("3"))
                .thenReturn(Mono.just(product3));

        // When & Then
        StepVerifier.create(service.getSimilarProducts(EXISTING_PRODUCT_ID))
                .expectNext(List.of(product2, product3))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when no similar products")
    void shouldReturnEmptyListWhenNoSimilarProducts() {
        // Given
        when(similarProductIdsPort.getSimilarIds(EXISTING_PRODUCT_ID))
                .thenReturn(Mono.just(List.of()));

        // When & Then
        StepVerifier.create(service.getSimilarProducts(EXISTING_PRODUCT_ID))
                .expectNext(List.of())
                .verifyComplete();

        verify(productDetailPort, never()).getProductDetail(anyString());
    }

    @Test
    @DisplayName("Should propagate exception when product not found")
    void shouldPropagateExceptionWhenProductNotFound() {
        // Given
        when(similarProductIdsPort.getSimilarIds(NON_EXISTING_PRODUCT_ID))
                .thenReturn(Mono.error(new ProductNotFoundException(NON_EXISTING_PRODUCT_ID)));

        // When & Then
        StepVerifier.create(service.getSimilarProducts(NON_EXISTING_PRODUCT_ID))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("Should skip products that fail to load")
    void shouldSkipProductsThatFailToLoad() {
        // Given
        List<String> similarIds = List.of("2", "3");
        Product product2 = new Product("2", "Product 2", new BigDecimal("19.99"), true);

        when(similarProductIdsPort.getSimilarIds(EXISTING_PRODUCT_ID))
                .thenReturn(Mono.just(similarIds));
        when(productDetailPort.getProductDetail("2"))
                .thenReturn(Mono.just(product2));
        when(productDetailPort.getProductDetail("3"))
                .thenReturn(Mono.error(new ProductNotFoundException("3")));

        // When & Then
        StepVerifier.create(service.getSimilarProducts(EXISTING_PRODUCT_ID))
                .expectNext(List.of(product2))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should fetch product details in parallel")
    void shouldFetchProductDetailsInParallel() {
        // Given
        List<String> similarIds = List.of("2", "3", "4");
        Product p2 = new Product("2", "P2", new BigDecimal("10"), true);
        Product p3 = new Product("3", "P3", new BigDecimal("20"), true);
        Product p4 = new Product("4", "P4", new BigDecimal("30"), true);

        when(similarProductIdsPort.getSimilarIds(EXISTING_PRODUCT_ID))
                .thenReturn(Mono.just(similarIds));

        // We simulate that each call takes 1 second
        when(productDetailPort.getProductDetail("2"))
                .thenReturn(Mono.just(p2).delayElement(Duration.ofSeconds(1)));
        when(productDetailPort.getProductDetail("3"))
                .thenReturn(Mono.just(p3).delayElement(Duration.ofSeconds(1)));
        when(productDetailPort.getProductDetail("4"))
                .thenReturn(Mono.just(p4).delayElement(Duration.ofSeconds(1)));

        // When & Then
        StepVerifier.withVirtualTime(() -> service.getSimilarProducts(EXISTING_PRODUCT_ID))
                .expectSubscription()
                .thenAwait(Duration.ofMillis(1100))
                .assertNext(products -> {
                    assert products.size() == 3;
                    assert products.get(0).id().equals("2");
                    assert products.get(2).id().equals("4");
                })
                .verifyComplete();

        verify(productDetailPort, times(3)).getProductDetail(anyString());
    }
}