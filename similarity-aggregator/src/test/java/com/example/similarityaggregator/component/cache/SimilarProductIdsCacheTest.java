package com.example.similarityaggregator.component.cache;

import com.example.similarityaggregator.infrastructure.rest.adapter.out.SimilarProductIdsRestAdapter;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import reactor.test.StepVerifier;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@WireMockTest(httpPort = 3001)
class SimilarProductIdsCacheTest {

    @Autowired
    private SimilarProductIdsRestAdapter adapter;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        WireMock.reset();

        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("Should return cached result on second call")
    void shouldReturnCachedResultOnSecondCall() {
        // Given
        String productId = "1";
        List<String> similarIds = List.of("2", "3");
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\", \"3\"]")));

        // When - first call (cache miss)
        StepVerifier.create(adapter.getSimilarIds(productId))
                .expectNext(similarIds)
                .verifyComplete();

        // When - second call (cache hit)
        StepVerifier.create(adapter.getSimilarIds(productId))
                .expectNext(similarIds)
                .verifyComplete();

        // Then - API called only once
        verify(1, getRequestedFor(urlEqualTo("/product/1/similarids")));
        var cache = cacheManager.getCache("similar-ids"); // Use your cache name
        Assert.assertNotNull(cache);
        Object cachedValue = cache.get(productId).get();
        assertThat(cachedValue).isEqualTo(similarIds);
    }

    @Test
    @DisplayName("Should call API for different product ids")
    void shouldCallApiForDifferentProductIds() {
        // Given
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\", \"3\"]")));

        stubFor(get(urlEqualTo("/product/2/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"4\", \"5\"]")));

        // When
        StepVerifier.create(adapter.getSimilarIds("1"))
                .expectNext(List.of("2", "3"))
                .verifyComplete();

        StepVerifier.create(adapter.getSimilarIds("2"))
                .expectNext(List.of("4", "5"))
                .verifyComplete();

        // Then
        verify(1, getRequestedFor(urlEqualTo("/product/1/similarids")));
        verify(1, getRequestedFor(urlEqualTo("/product/2/similarids")));
    }
}