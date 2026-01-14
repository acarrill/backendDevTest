package com.example.similarityaggregator.infrastructure.rest.adapter.in.dto;

public record ErrorResponse(
        String code,
        String message
) {}