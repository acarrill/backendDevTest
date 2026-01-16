package com.example.similarityaggregator.infrastructure.rest.adapter.in;

import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.netty.handler.timeout.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleProductNotFound(ProductNotFoundException ex) {
        log.info("Product not found: {}", ex.getProductId());
    }

    @ExceptionHandler(TimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public void handleTimeout(TimeoutException ex) {
        log.error("Upstream timeout: {}", ex.getMessage());
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