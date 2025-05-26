package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.helper.UserMappingHelper;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.User;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private final UserRepository userRepository;
	
	@Override
	public List<UserDto> findAll() {
		log.info("*** UserDto List, service; fetch all users *");
		return this.userRepository.findAll()
				.stream()
					.map(UserMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public UserDto findById(final Integer userId) {
		log.info("*** UserDto, service; fetch user by id *");
		return this.userRepository.findById(userId)
				.map(UserMappingHelper::map)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
	}
	
	@Override
	public UserDto save(final UserDto userDto) {
		log.info("*** UserDto, service; save user *");
		User user = UserMappingHelper.map(userDto);
		
		// Handle new User with new Credential to ensure User ID is generated first
		if (user.getUserId() == null && user.getCredential() != null && user.getCredential().getCredentialId() == null) {
			log.info("*** Handling new User with new Credential ***");
			Credential credential = user.getCredential();
			user.setCredential(null); // Temporarily detach credential
			
			// Save User to generate its ID
			User savedUser = this.userRepository.save(user);
			
			// Link Credential to the saved User (which now has an ID)
			credential.setUser(savedUser); // This sets the foreign key on Credential
			// Link saved User back to Credential for bidirectional consistency and cascading
			savedUser.setCredential(credential);
			
			// Save the User again; cascade should now persist Credential correctly
			// The User entity is already managed, so this save will cascade to the new Credential
			User finalSavedUser = this.userRepository.save(savedUser);
			return UserMappingHelper.map(finalSavedUser);
		} else {
			// For updates, or if User/Credential already has an ID, or no new credential
			log.info("*** Handling update or user without new credential ***");
			User savedUser = this.userRepository.save(user);
			return UserMappingHelper.map(savedUser);
		}
	}
	
	@Override
	public UserDto update(final UserDto userDto) {
		log.info("*** UserDto, service; update user *");
		
		final var user = this.userRepository.findById(userDto.getUserId())
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userDto.getUserId())));
		
		user.setFirstName(userDto.getFirstName());
		user.setLastName(userDto.getLastName());
		user.setEmail(userDto.getEmail());
		user.setPhone(userDto.getPhone());
		
		if (userDto.getCredentialDto() != null) {
			var existingCredential = user.getCredential();
			if (existingCredential == null) {
				existingCredential = Credential.builder()
						.username(userDto.getCredentialDto().getUsername())
						.password(userDto.getCredentialDto().getPassword())
						.roleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority())
						.isEnabled(userDto.getCredentialDto().getIsEnabled())
						.isAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired())
						.isAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked())
						.isCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired())
						.user(user)
						.build();
				user.setCredential(existingCredential);
			} else {
				existingCredential.setUsername(userDto.getCredentialDto().getUsername());
				// Password should only be updated if a new one is provided
				if (userDto.getCredentialDto().getPassword() != null && !userDto.getCredentialDto().getPassword().isBlank()) {
					existingCredential.setPassword(userDto.getCredentialDto().getPassword());
				}
				existingCredential.setRoleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority());
				existingCredential.setIsEnabled(userDto.getCredentialDto().getIsEnabled());
				existingCredential.setIsAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired());
				existingCredential.setIsAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked());
				existingCredential.setIsCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired());
			}
		}
		
		return UserMappingHelper.map(this.userRepository.save(user));
	}
	
	@Override
	public UserDto update(final Integer userId, final UserDto userDto) {
		log.info("*** UserDto, service; update user with userId *");
		
		final var user = this.userRepository.findById(userId)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with id: %d not found", userId)));
		
		user.setFirstName(userDto.getFirstName());
		user.setLastName(userDto.getLastName());
		user.setEmail(userDto.getEmail());
		user.setPhone(userDto.getPhone());
		
		if (userDto.getCredentialDto() != null) {
			var existingCredential = user.getCredential();
			if (existingCredential == null) {
				existingCredential = Credential.builder()
						.username(userDto.getCredentialDto().getUsername())
						.password(userDto.getCredentialDto().getPassword())
						.roleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority())
						.isEnabled(userDto.getCredentialDto().getIsEnabled())
						.isAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired())
						.isAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked())
						.isCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired())
						.user(user)
						.build();
				user.setCredential(existingCredential);
			} else {
				existingCredential.setUsername(userDto.getCredentialDto().getUsername());
				// Password should only be updated if a new one is provided
				if (userDto.getCredentialDto().getPassword() != null && !userDto.getCredentialDto().getPassword().isBlank()) {
					existingCredential.setPassword(userDto.getCredentialDto().getPassword());
				}
				existingCredential.setRoleBasedAuthority(userDto.getCredentialDto().getRoleBasedAuthority());
				existingCredential.setIsEnabled(userDto.getCredentialDto().getIsEnabled());
				existingCredential.setIsAccountNonExpired(userDto.getCredentialDto().getIsAccountNonExpired());
				existingCredential.setIsAccountNonLocked(userDto.getCredentialDto().getIsAccountNonLocked());
				existingCredential.setIsCredentialsNonExpired(userDto.getCredentialDto().getIsCredentialsNonExpired());
			}
		}
		
		return UserMappingHelper.map(this.userRepository.save(user));
	}
	
	@Override
	public void deleteById(final Integer userId) {
		log.info("*** Void, service; delete user by id *");
		this.userRepository.deleteById(userId);
	}
	
	@Override
	public UserDto findByUsername(final String username) {
		log.info("*** UserDto, service; fetch user with username *");
		return UserMappingHelper.map(this.userRepository.findByCredentialUsername(username)
				.orElseThrow(() -> new UserObjectNotFoundException(String.format("User with username: %s not found", username))));
	}
	
	
	
}










