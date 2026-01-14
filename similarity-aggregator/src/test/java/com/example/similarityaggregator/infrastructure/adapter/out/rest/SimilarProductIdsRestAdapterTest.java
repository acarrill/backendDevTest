package com.example.similarityaggregator.infrastructure.adapter.out.rest;

import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
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

import java.io.IOException;
import java.util.List;

class SimilarProductIdsRestAdapterTest {

    private static final String EXISTING_PRODUCT_ID = "1";
    private static final String NON_EXISTING_PRODUCT_ID = "999";
    private static final List<String> SIMILAR_IDS = List.of("2", "3", "4");

    private MockWebServer mockWebServer;
    private SimilarProductIdsRestAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        adapter = new SimilarProductIdsRestAdapter(WebClient.builder(), baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should return similar product ids")
    void shouldReturnSimilarProductIds() {
        // Given
        enqueueSuccessResponse("[\"2\", \"3\", \"4\"]");

        // When & Then
        StepVerifier.create(adapter.getSimilarIds(EXISTING_PRODUCT_ID))
                .expectNext(SIMILAR_IDS)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when 404")
    void shouldThrowProductNotFoundExceptionWhen404() {
        // Given
        enqueueNotFoundResponse();

        // When & Then
        StepVerifier.create(adapter.getSimilarIds(NON_EXISTING_PRODUCT_ID))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    private void enqueueSuccessResponse(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
    }

    private void enqueueNotFoundResponse() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
    }
}