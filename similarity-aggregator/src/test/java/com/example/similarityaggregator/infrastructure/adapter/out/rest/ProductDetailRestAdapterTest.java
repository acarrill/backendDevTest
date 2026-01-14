// src/test/java/com/example/similarityaggregator/infrastructure/adapter/out/rest/ProductDetailRestAdapterTest.java
package com.example.similarityaggregator.infrastructure.adapter.out.rest;

import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.domain.model.Product;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;

class ProductDetailRestAdapterTest {

    private static final String EXISTING_PRODUCT_ID = "1";
    private static final String NON_EXISTING_PRODUCT_ID = "999";

    private MockWebServer mockWebServer;
    private ProductDetailRestAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        adapter = new ProductDetailRestAdapter(WebClient.builder(), baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should call correct URL")
    void shouldCallCorrectUrl() throws InterruptedException {
        // Given
        enqueueSuccessResponse("""
            {
                "id": "1",
                "name": "Product 1",
                "price": 19.99,
                "availability": true
            }
            """);

        // When
        StepVerifier.create(adapter.getProductDetail("1"))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/product/1");
        assertThat(request.getMethod()).isEqualTo("GET");
    }

    @Test
    @DisplayName("Should return product detail")
    void shouldReturnProductDetail() {
        // Given
        enqueueSuccessResponse("""
                {
                    "id": "1",
                    "name": "Product 1",
                    "price": 19.99,
                    "availability": true
                }
                """);

        Product expected = new Product("1", "Product 1", new BigDecimal("19.99"), true);

        // When & Then
        StepVerifier.create(adapter.getProductDetail(EXISTING_PRODUCT_ID))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when 404")
    void shouldThrowProductNotFoundExceptionWhen404() {
        // Given
        enqueueNotFoundResponse();

        // When & Then
        StepVerifier.create(adapter.getProductDetail(NON_EXISTING_PRODUCT_ID))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    private void enqueueSuccessResponse(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @DisplayName("Should return empty when response body is empty")
    void shouldReturnEmptyWhenResponseBodyIsEmpty() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(""));

        // When & Then
        StepVerifier.create(adapter.getProductDetail(EXISTING_PRODUCT_ID))
                .verifyComplete();
    }

    private void enqueueNotFoundResponse() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
    }

}