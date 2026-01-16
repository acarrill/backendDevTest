package com.example.similarityaggregator.component.circuitbreaker;

import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.infrastructure.rest.adapter.out.ProductDetailRestAdapter;
import com.example.similarityaggregator.infrastructure.rest.adapter.out.SimilarProductIdsRestAdapter;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "similar-products.api.base-url=http://localhost:3001",
        "resilience4j.circuitbreaker.instances.similarIds.slidingWindowSize=4",
        "resilience4j.circuitbreaker.instances.similarIds.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.similarIds.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.similarIds.waitDurationInOpenState=10s",
        "resilience4j.circuitbreaker.instances.similarIds.permitted-number-of-calls-in-half-open-state=1",
        "resilience4j.circuitbreaker.instances.productDetail.slidingWindowSize=4",
        "resilience4j.circuitbreaker.instances.productDetail.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.productDetail.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.productDetail.waitDurationInOpenState=10s",
        "resilience4j.circuitbreaker.instances.productDetail.permitted-number-of-calls-in-half-open-state=1"
})
@WireMockTest(httpPort = 3001)
class CircuitBreakerTest {

    @Autowired
    private SimilarProductIdsRestAdapter similarProductIdsRestAdapter;

    @Autowired
    private ProductDetailRestAdapter productDetailAdapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker similarIdsCircuitBreaker;

    private CircuitBreaker productDetailCircuitBreaker;

    @BeforeEach
    void setUp() {
        WireMock.reset();
        similarIdsCircuitBreaker = circuitBreakerRegistry.circuitBreaker("similarIds");
        productDetailCircuitBreaker = circuitBreakerRegistry.circuitBreaker("productDetail");
        similarIdsCircuitBreaker.reset();
        productDetailCircuitBreaker.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Should be closed initially")
    void shouldBeClosedInitially() {
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Should stay closed on successful calls")
    void shouldStayClosedOnSuccessfulCalls() {
        // Given
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\", \"3\"]")));

        // When
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(similarProductIdsRestAdapter.getSimilarIds("1"))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        // Then
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Should open after failure threshold reached")
    void shouldOpenAfterFailureThresholdReached() {
        // Given
        stubFor(get(urlPathMatching("/product/.*/similarids"))
                .willReturn(aResponse().withStatus(500)));

        // When
        int minimumCalls = 4;
        for (int i = 0; i < minimumCalls; i++) {
            similarProductIdsRestAdapter.getSimilarIds(String.valueOf(i))
                    .onErrorComplete()
                    .block();
        }

        // Then
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should ignore Not Found errors and stay CLOSED")
    void shouldStayClosedOnConsecutiveNotFoundSimilarIds() {
        // Given
        stubFor(get(urlPathMatching("/product/.*/similarids"))
                .willReturn(notFound()));

        final int totalCalls = similarIdsCircuitBreaker.getCircuitBreakerConfig()
                .getMinimumNumberOfCalls() + 5;

        // When
        StepVerifier.create(
            Flux.range(0, totalCalls)
                .flatMap(i -> similarProductIdsRestAdapter.getSimilarIds(String.valueOf(i))
                    .onErrorResume(ProductNotFoundException.class, e -> Mono.empty())
                ).then()
        ).verifyComplete();

        // Then
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(similarIdsCircuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    @DisplayName("Should stay closed on not found errors in product detail")
    void shouldStayClosedOnConsecutiveNotFoundProductDetail() {
        stubFor(get(urlPathMatching("/product/1"))
                .willReturn(aResponse().withStatus(404)));

        final int totalCalls = similarIdsCircuitBreaker.getCircuitBreakerConfig().getMinimumNumberOfCalls() + 5;

        Flux.range(0, totalCalls)
                .flatMap(i -> productDetailAdapter.getProductDetail(String.valueOf(i))
                        .onErrorComplete())
                .as(StepVerifier::create)
                .verifyComplete();

        assertThat(productDetailCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(productDetailCircuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    @DisplayName("Should fail fast when circuit is open")
    void shouldFailFastWhenCircuitIsOpen() {
        // Given
        similarIdsCircuitBreaker.transitionToOpenState();

        // When
        long startTime = System.currentTimeMillis();

        StepVerifier.create(similarProductIdsRestAdapter.getSimilarIds("1"))
                .expectError();

        long elapsed = System.currentTimeMillis() - startTime;

        // Then
        assertThat(elapsed).isLessThan(100);

        verify(0, getRequestedFor(urlEqualTo("/product/1/similarids")));
    }

    @Test
    @DisplayName("Should transition to half-open after wait duration")
    void shouldTransitionToHalfOpenAfterWaitDuration() {
        // Given
        similarIdsCircuitBreaker.transitionToOpenState();
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When
        similarIdsCircuitBreaker.transitionToHalfOpenState();

        // Then
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Should close again after successful calls in half-open state")
    void shouldCloseAfterSuccessfulCallsInHalfOpenState() {
        // Given
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\", \"3\"]")));

        similarIdsCircuitBreaker.transitionToOpenState();
        similarIdsCircuitBreaker.transitionToHalfOpenState();

        // When
        StepVerifier.create(similarProductIdsRestAdapter.getSimilarIds("1"))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        assertThat(similarIdsCircuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}