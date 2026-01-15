package com.example.similarityaggregator.application.service;

import com.example.similarityaggregator.application.port.in.GetSimilarProductsUseCase;
import com.example.similarityaggregator.application.port.out.ProductDetailPort;
import com.example.similarityaggregator.application.port.out.SimilarProductIdsPort;
import com.example.similarityaggregator.domain.exception.ProductNotFoundException;
import com.example.similarityaggregator.domain.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class GetSimilarProductsService implements GetSimilarProductsUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSimilarProductsService.class);

    private final SimilarProductIdsPort similarProductIdsPort;
    private final ProductDetailPort productDetailPort;

    public GetSimilarProductsService(SimilarProductIdsPort similarProductIdsPort,
                                     ProductDetailPort productDetailPort) {
        this.similarProductIdsPort = similarProductIdsPort;
        this.productDetailPort = productDetailPort;
    }

    @Override
    public Mono<List<Product>> getSimilarProducts(String productId) {
        log.info("Fetching similar products for productId={}", productId);

        return similarProductIdsPort.getSimilarIds(productId)
                .doOnNext(ids -> log.debug("Found {} similar ids", ids.size()))
                .flatMapMany(this::fetchProductsPreservingOrder)
                .collectList()
                .doOnSuccess(products -> log.info("Returning {} products for productId={}", products.size(), productId))
                .cache();
    }

    private Flux<Product> fetchProductsPreservingOrder(List<String> productIds) {
        return Flux.fromIterable(productIds)
                .flatMapSequential(this::fetchProductOrSkip, 10);
    }

    private Mono<Product> fetchProductOrSkip(String productId) {
        return productDetailPort.getProductDetail(productId)
                .doOnNext(product -> log.debug("Fetched product: {}", product.id()))
                .onErrorResume(ProductNotFoundException.class, e -> {
                    log.debug("Product not found, skipping: {}", productId);
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    log.error("Unexpected error fetching product {}: {}", productId, e.getMessage());
                    return Mono.empty();
                });
    }
}