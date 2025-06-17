package com.selimhorri.app.business.user.service;

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

import com.selimhorri.app.business.user.model.VerificationTokenDto;
import com.selimhorri.app.business.user.model.response.VerificationUserTokenServiceCollectionDtoResponse;

@FeignClient(name = "USER-SERVICE", contextId = "verificationTokenClientService", path = "/user-service/api/verificationTokens", decode404 = true)
public interface VerificationTokenClientService {
	
	@GetMapping
	@Retry(name = "user-service")
	ResponseEntity<VerificationUserTokenServiceCollectionDtoResponse> findAll();
	
	@GetMapping("/{verificationTokenId}")
	@Retry(name = "user-service")
	ResponseEntity<VerificationTokenDto> findById(
			@PathVariable("verificationTokenId") 
			@NotBlank(message = "*Input must not blank!**") 
			@Valid final String verificationTokenId);
	
	@PostMapping
	@Retry(name = "user-service")
	ResponseEntity<VerificationTokenDto> save(
			@RequestBody 
			@NotNull(message = "*Input must not NULL!**") 
			@Valid final VerificationTokenDto verificationTokenDto);
	
	@PutMapping
	@Retry(name = "user-service")
	ResponseEntity<VerificationTokenDto> update(
			@RequestBody 
			@NotNull(message = "*Input must not NULL!**") 
			@Valid final VerificationTokenDto verificationTokenDto);
	
	@PutMapping("/{verificationTokenId}")
	@Retry(name = "user-service")
	ResponseEntity<VerificationTokenDto> update(
			@PathVariable("verificationTokenId") 
			@NotBlank(message = "*Input must not blank!**") final String verificationTokenId, 
			@RequestBody 
			@NotNull(message = "*Input must not NULL!**") 
			@Valid final VerificationTokenDto verificationTokenDto);
	
	@DeleteMapping("/{verificationTokenId}")
	@Retry(name = "user-service")
	ResponseEntity<Boolean> deleteById(@PathVariable("verificationTokenId") @NotBlank(message = "*Input must not blank!**") @Valid final String verificationTokenId);
	
}










