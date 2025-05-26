package com.selimhorri.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Address;
import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.AddressDto;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.UserObjectNotFoundException;
import com.selimhorri.app.repository.AddressRepository;
import com.selimhorri.app.repository.CredentialRepository;
import com.selimhorri.app.repository.UserRepository;
import com.selimhorri.app.service.impl.AddressServiceImpl;
import com.selimhorri.app.service.impl.CredentialServiceImpl;
import com.selimhorri.app.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceApplicationTests {
	
	@Mock
	private UserRepository userRepository;
	
	@Mock
	private AddressRepository addressRepository;
	
	@Mock
	private CredentialRepository credentialRepository;
	
	@InjectMocks
	private UserServiceImpl userService;
	
	@InjectMocks
	private AddressServiceImpl addressService;
	
	@InjectMocks
	private CredentialServiceImpl credentialService;
	@Test
	@DisplayName("Test 1: Find all users - Success scenario")
	void testFindAllUsers() {
		List<User> users = new ArrayList<>();
		
		Credential credential1 = Credential.builder()
				.credentialId(1)
				.username("johndoe")
				.password("password123")
				.roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
				.isEnabled(true)
				.isAccountNonExpired(true)
				.isAccountNonLocked(true)
				.isCredentialsNonExpired(true)
				.build();
				
		User user1 = User.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.phone("123456789")
				.credential(credential1)
				.build();
		
		Credential credential2 = Credential.builder()
				.credentialId(2)
				.username("janesmith")
				.password("password456")
				.roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
				.isEnabled(true)
				.isAccountNonExpired(true)
				.isAccountNonLocked(true)
				.isCredentialsNonExpired(true)
				.build();
				
		User user2 = User.builder()
				.userId(2)
				.firstName("Jane")
				.lastName("Smith")
				.email("jane@example.com")
				.phone("987654321")
				.credential(credential2)
				.build();
				
		credential1.setUser(user1);
		credential2.setUser(user2);
		
		users.add(user1);
		users.add(user2);
		
		when(userRepository.findAll()).thenReturn(users);
		
		List<UserDto> result = userService.findAll();
		
		assertEquals(2, result.size());
		assertEquals("John", result.get(0).getFirstName());
		assertEquals("Jane", result.get(1).getFirstName());
		verify(userRepository, times(1)).findAll();
	}
		@Test
	@DisplayName("Test 2: Find user by ID - Success scenario")
	void testFindUserById_Success() {

		Credential credential = Credential.builder()
				.credentialId(1)
				.username("johndoe")
				.password("password123")
				.roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
				.isEnabled(true)
				.isAccountNonExpired(true)
				.isAccountNonLocked(true)
				.isCredentialsNonExpired(true)
				.build();
				
		User user = User.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.email("john@example.com")
				.phone("123456789")
				.credential(credential)
				.build();
		
		credential.setUser(user);
		
		when(userRepository.findById(1)).thenReturn(Optional.of(user));
		
		UserDto result = userService.findById(1);
		
		assertNotNull(result);
		assertEquals(1, result.getUserId());
		assertEquals("John", result.getFirstName());
		assertEquals("john@example.com", result.getEmail());
		verify(userRepository, times(1)).findById(1);
	}
	
	@Test
	@DisplayName("Test 3: Find user by ID - User not found")
	void testFindUserById_NotFound() {
		when(userRepository.findById(999)).thenReturn(Optional.empty());

		assertThrows(UserObjectNotFoundException.class, () -> userService.findById(999));
		verify(userRepository, times(1)).findById(999);
	}
	
	@Test
	@DisplayName("Test 4: Find address by ID - Success scenario")
	void testFindAddressById_Success() {
		User user = User.builder().userId(1).firstName("John").lastName("Doe").build();
		Address address = Address.builder()
				.addressId(1)
				.fullAddress("123 Main St")
				.postalCode("12345")
				.city("New York")
				.user(user)
				.build();
		
		when(addressRepository.findById(1)).thenReturn(Optional.of(address));
		
		AddressDto result = addressService.findById(1);
		
		assertNotNull(result);
		assertEquals(1, result.getAddressId());
		assertEquals("123 Main St", result.getFullAddress());
		assertEquals("New York", result.getCity());
		verify(addressRepository, times(1)).findById(1);
	}
	
	@Test
	@DisplayName("Test 5: Find credential by username - Success scenario")
	void testFindCredentialByUsername_Success() {

		User user = User.builder().userId(1).firstName("John").lastName("Doe").build();
		Credential credential = Credential.builder()
				.credentialId(1)
				.username("johndoe")
				.password("password123")
				.user(user)
				.build();
		
		when(credentialRepository.findByUsername("johndoe")).thenReturn(Optional.of(credential));

		CredentialDto result = credentialService.findByUsername("johndoe");
		
		assertNotNull(result);
		assertEquals(1, result.getCredentialId());
		assertEquals("johndoe", result.getUsername());
		verify(credentialRepository, times(1)).findByUsername("johndoe");
	}
}

