package com.example.similarityaggregator.infrastructure.rest.adapter.in;

import com.example.similarityaggregator.application.port.in.GetSimilarProductsUseCase;
import com.example.similarityaggregator.infrastructure.rest.adapter.in.dto.ProductResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/product")
public class SimilarProductsController {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;

    public SimilarProductsController(GetSimilarProductsUseCase getSimilarProductsUseCase) {
        this.getSimilarProductsUseCase = getSimilarProductsUseCase;
    }

    @GetMapping("/{productId}/similar")
    public Mono<List<ProductResponse>> getSimilarProducts(@PathVariable String productId) {
        return getSimilarProductsUseCase.getSimilarProducts(productId)
                .map(products -> products.stream()
                        .map(ProductResponse::fromDomain)
                        .toList());
    }
}