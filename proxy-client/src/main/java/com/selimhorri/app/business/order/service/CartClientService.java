package com.selimhorri.app.business.order.service;

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

import com.selimhorri.app.business.order.model.CartDto;
import com.selimhorri.app.business.order.model.response.CartOrderServiceDtoCollectionResponse;

@FeignClient(name = "ORDER-SERVICE", contextId = "cartClientService", path = "/order-service/api/carts")
public interface CartClientService {
	
	@GetMapping
	@Retry(name = "order-service")
	public ResponseEntity<CartOrderServiceDtoCollectionResponse> findAll();
	
	@GetMapping("/{cartId}")
	@Retry(name = "order-service")
	public ResponseEntity<CartDto> findById(
			@PathVariable("cartId") 
			@NotBlank(message = "Input must not be blank!") 
			@Valid final String cartId);
	
	@PostMapping
	@Retry(name = "order-service")
	public ResponseEntity<CartDto> save(
			@RequestBody 
			@NotNull(message = "Input must not be NULL!") 
			@Valid final CartDto cartDto);
	
	@PutMapping
	@Retry(name = "order-service")
	public ResponseEntity<CartDto> update(
			@RequestBody 
			@NotNull(message = "Input must not be NULL!") 
			@Valid final CartDto cartDto);
	
	@PutMapping("/{cartId}")
	@Retry(name = "order-service")
	public ResponseEntity<CartDto> update(
			@PathVariable("cartId")
			@NotBlank(message = "Input must not be blank!")
			@Valid final String cartId,
			@RequestBody 
			@NotNull(message = "Input must not be NULL!") 
			@Valid final CartDto cartDto);
	
	@DeleteMapping("/{cartId}")
	@Retry(name = "order-service")
	public ResponseEntity<Boolean> deleteById(@PathVariable("cartId") final String cartId);
	
}










