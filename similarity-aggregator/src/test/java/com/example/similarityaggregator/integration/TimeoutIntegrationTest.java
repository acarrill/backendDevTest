package com.example.similarityaggregator.integration;

import com.example.similarityaggregator.infrastructure.rest.config.SimilarProductsApiProperties;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@WireMockTest(httpPort = 3001)
@ActiveProfiles("test")
@EnableConfigurationProperties(SimilarProductsApiProperties.class)
class TimeoutIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(TimeoutIntegrationTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private SimilarProductsApiProperties properties;

    @Test
    @DisplayName("Should timeout when similar ids API is slow")
    void shouldTimeoutWhenSimilarIdsApiIsSlow() {
        // Given
        long timeoutMillis = properties.timeout().toMillis();
        long delayMillis = timeoutMillis + 2000;

        log.info("Configured timeout: {}ms, delay: {}ms", timeoutMillis, delayMillis);

        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\"]")
                        .withFixedDelay((int) delayMillis)));

        // When
        long startTime = System.currentTimeMillis();

        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().is5xxServerError();

        long elapsed = System.currentTimeMillis() - startTime;

        // Then
        log.info("Elapsed: {}ms", elapsed);

        assertThat(elapsed)
                .isGreaterThanOrEqualTo(timeoutMillis - 500)
                .isLessThan(delayMillis);
    }

    @Test
    @DisplayName("Should skip product when detail API is slow")
    void shouldSkipProductWhenDetailApiIsSlow() {
        // Given
        long delayMillis = properties.timeout().toMillis() + 2000;

        stubFor(get(urlEqualTo("/product/1/similarids"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"2\", \"3\"]")));

        stubFor(get(urlEqualTo("/product/2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "2",
                                    "name": "Product 2",
                                    "price": 19.99,
                                    "availability": true
                                }
                                """)));

        stubFor(get(urlEqualTo("/product/3"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "3",
                                    "name": "Product 3",
                                    "price": 29.99,
                                    "availability": false
                                }
                                """)
                        .withFixedDelay((int) delayMillis)));

        // When & Then
        webTestClient.get()
                .uri("/product/1/similar")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo("2");
    }
}