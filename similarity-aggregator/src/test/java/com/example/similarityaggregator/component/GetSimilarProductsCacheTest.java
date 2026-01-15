package com.example.similarityaggregator.component;

import com.example.similarityaggregator.application.port.out.ProductDetailPort;
import com.example.similarityaggregator.application.port.out.SimilarProductIdsPort;
import com.example.similarityaggregator.application.service.GetSimilarProductsService;
import com.example.similarityaggregator.domain.model.Product;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class GetSimilarProductsCacheTest {

    @Autowired
    private GetSimilarProductsService service;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private SimilarProductIdsPort idsPort;

    @MockitoBean
    private ProductDetailPort detailPort;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames()
            .forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("Should retrieve data from cache on second call")
    void shouldRetrieveFromCacheOnSecondCall() {
        // Given
        String productId = "1";
        Product product = new Product("2", "Test", new BigDecimal("10.0"), true);

        when(idsPort.getSimilarIds(productId)).thenReturn(Mono.just(List.of("2")));
        when(detailPort.getProductDetail("2")).thenReturn(Mono.just(product));

        // When: First Call (Cache Miss)
        StepVerifier.create(service.getSimilarProducts(productId))
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();

        // When: Second Call (Cache Hit)
        StepVerifier.create(service.getSimilarProducts(productId))
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();

        // Then
        verify(idsPort, times(1)).getSimilarIds(productId);
        var cache = cacheManager.getCache("product-details"); // Use your cache name
        Assert.assertNotNull(cache);
        Object cachedValue = cache.get(productId).get();
        assertThat(cachedValue).isEqualTo(List.of(product));
    }

    @Test
    @DisplayName("Should not hit cache for different product IDs")
    void shouldNotHitCache() {
        // Given
        String productId = "1";
        String productIdSecondCall = "2";

        when(idsPort.getSimilarIds(productId)).thenReturn(Mono.just(List.of("2")));
        when(detailPort.getProductDetail("2")).thenReturn(Mono.just(new Product("2", "Test", new BigDecimal("10.0"), true)));
        when(idsPort.getSimilarIds(productIdSecondCall)).thenReturn(Mono.just(List.of("2")));

        // When: First Call (Cache Miss)
        StepVerifier.create(service.getSimilarProducts(productId))
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();

        // When: Second Call (Cache Miss)
        StepVerifier.create(service.getSimilarProducts(productIdSecondCall))
                .expectNextMatches(list -> list.size() == 1)
                .verifyComplete();

        // Then
        verify(idsPort, times(1)).getSimilarIds(productId);
        verify(idsPort, times(1)).getSimilarIds(productIdSecondCall);
    }
}