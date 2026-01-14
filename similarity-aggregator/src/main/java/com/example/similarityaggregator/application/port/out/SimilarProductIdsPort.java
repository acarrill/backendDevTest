// application/port/out/SimilarProductIdsPort.java
package com.example.similarityaggregator.application.port.out;

import reactor.core.publisher.Mono;

import java.util.List;

public interface SimilarProductIdsPort {

    Mono<List<String>> getSimilarIds(String productId);
}