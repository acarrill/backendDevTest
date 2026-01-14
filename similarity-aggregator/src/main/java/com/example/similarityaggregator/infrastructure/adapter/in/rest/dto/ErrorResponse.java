package com.example.similarityaggregator.infrastructure.adapter.in.rest.dto;

public record ErrorResponse(
        String code,
        String message
) {}