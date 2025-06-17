package com.selimhorri.app.business.product.service;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

import com.selimhorri.app.business.product.model.ProductDto;
import com.selimhorri.app.business.product.model.response.ProductProductServiceCollectionDtoResponse;

@FeignClient(name = "PRODUCT-SERVICE", contextId = "productClientService", path = "/product-service/api/products")
public interface ProductClientService {

	@GetMapping
	@Retry(name = "product-service")
	@TimeLimiter(name = "product-service")
	ResponseEntity<ProductProductServiceCollectionDtoResponse> findAll();

	@GetMapping("/{productId}")
	@Retry(name = "product-service")
	@TimeLimiter(name = "product-service")
	ResponseEntity<ProductDto> findById(
			@PathVariable("productId") 
			@NotBlank(message = "Input must not be blank!") 
			@Valid final String productId);

	@PostMapping
	@Retry(name = "product-service")
	@TimeLimiter(name = "product-service")
	ResponseEntity<ProductDto> save(
			@RequestBody 
			@NotNull(message = "Input must not be NULL!") 
			@Valid final ProductDto productDto);

	@PutMapping
	@Retry(name = "product-service")
	@TimeLimiter(name = "product-service")
	ResponseEntity<ProductDto> update(
			@RequestBody 
			@NotNull(message = "Input must not be NULL!") 
			@Valid final ProductDto productDto);

	@PutMapping("/{productId}")
	@Retry(name = "product-service")
	@TimeLimiter(name = "product-service")
	ResponseEntity<ProductDto> update(
			@PathVariable("productId")
			@NotBlank(message = "Input must not be blank!")
			@Valid final String productId,
			@RequestBody 
			@NotNull(message = "Input must not be NULL!") 
			@Valid final ProductDto productDto);
			
	@DeleteMapping("/{productId}")
	@Retry(name = "product-service")
	@TimeLimiter(name = "product-service")
	ResponseEntity<Boolean> deleteById(@PathVariable("productId") final String productId);
	
}










