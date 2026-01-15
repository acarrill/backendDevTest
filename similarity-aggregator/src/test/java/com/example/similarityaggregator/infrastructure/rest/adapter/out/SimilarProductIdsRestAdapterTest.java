package com.example.similarityaggregator.infrastructure.rest.adapter.out;

import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.infrastructure.rest.adapter.out.SimilarProductIdsRestAdapter;
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

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("Should call correct URL")
    void shouldCallCorrectUrl() throws InterruptedException {
        // Given
        enqueueSuccessResponse("[\"2\", \"3\", \"4\"]");

        // When
        StepVerifier.create(adapter.getSimilarIds("1"))
                .expectNextCount(1)
                .verifyComplete();

        // Then
        var request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/product/1/similarids");
        assertThat(request.getMethod()).isEqualTo("GET");
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

    @Test
    @DisplayName("Should return empty list when response body is empty array")
    void shouldReturnEmptyListWhenResponseBodyIsEmptyArray() {
        // Given
        enqueueSuccessResponse("[]");

        // When & Then
        StepVerifier.create(adapter.getSimilarIds(EXISTING_PRODUCT_ID))
                .expectNext(List.of())
                .verifyComplete();
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