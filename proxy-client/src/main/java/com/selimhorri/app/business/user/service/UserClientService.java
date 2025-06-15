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

import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.model.response.UserUserServiceCollectionDtoResponse;

@FeignClient(name = "USER-SERVICE", contextId = "userClientService", path = "/user-service/api/users", decode404 = true)
public interface UserClientService {
	
	@GetMapping
	@Retry(name = "user-service")
	ResponseEntity<UserUserServiceCollectionDtoResponse> findAll();
	
	@GetMapping("/{userId}")
	@Retry(name = "user-service")
	ResponseEntity<UserDto> findById(
			@PathVariable("userId") 
			@NotBlank(message = "*Input must not blank!**") 
			@Valid final String userId);
	
	@GetMapping("/username/{username}")
	@Retry(name = "user-service")
	ResponseEntity<UserDto> findByUsername(
			@PathVariable("username") 
			@NotBlank(message = "*Input must not blank!**") 
			@Valid final String username);
	
	@PostMapping
	@Retry(name = "user-service")
	ResponseEntity<UserDto> save(
			@RequestBody 
			@NotNull(message = "*Input must not NULL!**") 
			@Valid final UserDto userDto);
	
	@PutMapping
	@Retry(name = "user-service")
	ResponseEntity<UserDto> update(
			@RequestBody 
			@NotNull(message = "*Input must not NULL!**") 
			@Valid final UserDto userDto);
	
	@PutMapping("/{userId}")
	@Retry(name = "user-service")
	ResponseEntity<UserDto> update(
			@PathVariable("userId") 
			@NotBlank(message = "*Input must not blank!**") final String userId, 
			@RequestBody 
			@NotNull(message = "*Input must not NULL!**") 
			@Valid final UserDto userDto);
	
	@DeleteMapping("/{userId}")
	@Retry(name = "user-service")
	ResponseEntity<Boolean> deleteById(@PathVariable("userId") @NotBlank(message = "*Input must not blank!**") @Valid final String userId);
	
}










