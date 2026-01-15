package com.example.similarityaggregator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SimilarityAggregatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimilarityAggregatorApplication.class, args);
	}

}
