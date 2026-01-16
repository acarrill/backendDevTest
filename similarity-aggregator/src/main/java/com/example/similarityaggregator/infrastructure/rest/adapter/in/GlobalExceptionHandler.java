package com.example.similarityaggregator.infrastructure.rest.adapter.in;

import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleProductNotFound(ProductNotFoundException ex) {
        log.info("Product not found: {}", ex.getProductId());
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<Void> handleWebClientException(WebClientRequestException ex) {
        if (ex.getCause() instanceof ReadTimeoutException) {
            log.error("Upstream request timeout - URI: {}", ex.getUri());
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();
        }

        log.error("Upstream connection error: {}", ex.getUri());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
    }

    @ExceptionHandler(CallNotPermittedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public void handleServiceUnavailable(Exception ex) {
        log.error("Service unavailable: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
    }
}