package com.example.similarityaggregator.component.circuitbreaker;

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
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "similar-products.api.base-url=http://localhost:3001",
        "resilience4j.circuitbreaker.instances.similarIds.slidingWindowSize=4",
        "resilience4j.circuitbreaker.instances.similarIds.minimumNumberOfCalls=2",
        "resilience4j.circuitbreaker.instances.similarIds.failureRateThreshold=50",
        "resilience4j.circuitbreaker.instances.similarIds.waitDurationInOpenState=10s",
        "resilience4j.circuitbreaker.instances.similarIds.permitted-number-of-calls-in-half-open-state: 1"
})
@WireMockTest(httpPort = 3001)
class CircuitBreakerTest {

    @Autowired
    private SimilarProductIdsRestAdapter adapter;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        WireMock.reset();
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("similarIds");
        circuitBreaker.reset();
    }

    @Test
    @Order(1)
    @DisplayName("Should be closed initially")
    void shouldBeClosedInitially() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
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
            StepVerifier.create(adapter.getSimilarIds("1"))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
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
            adapter.getSimilarIds(String.valueOf(i))
                    .onErrorComplete()
                    .block();
        }

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Should fail fast when circuit is open")
    void shouldFailFastWhenCircuitIsOpen() {
        // Given
        circuitBreaker.transitionToOpenState();

        // When
        long startTime = System.currentTimeMillis();

        StepVerifier.create(adapter.getSimilarIds("1"))
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
        circuitBreaker.transitionToOpenState();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // When
        circuitBreaker.transitionToHalfOpenState();

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Should close again after successful calls in half-open state")
    void shouldCloseAfterSuccessfulCallsInHalfOpenState() {
        // Given
        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\", \"3\"]")));

        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        // When
        StepVerifier.create(adapter.getSimilarIds("1"))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}