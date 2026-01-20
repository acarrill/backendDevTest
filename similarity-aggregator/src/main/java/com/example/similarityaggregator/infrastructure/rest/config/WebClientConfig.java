package com.example.similarityaggregator.infrastructure.rest.config;

import com.example.similarityaggregator.infrastructure.rest.filter.WebClientTimingFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SimilarProductsApiProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient similarProductsWebClient(WebClient.Builder webClientBuilder, SimilarProductsApiProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(properties.timeout().toMillis()));

        return webClientBuilder
                .clone()
                .baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}