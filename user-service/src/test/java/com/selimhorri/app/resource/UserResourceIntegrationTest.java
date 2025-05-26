package com.selimhorri.app.resource;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows; // Added import
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.NestedServletException; // Added import

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.Credential;
import com.selimhorri.app.domain.RoleBasedAuthority;
import com.selimhorri.app.domain.User;
import com.selimhorri.app.dto.CredentialDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
@Transactional
public class UserResourceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private UserDto testUserDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        userRepository.deleteAll();
        
        setupTestData();
    }

    private void setupTestData() {
        Credential testCredential = Credential.builder()
                .username("testuser")
                .password("testpassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testUser = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@example.com")
                .phone("1234567890")
                .imageUrl("http://example.com/image.jpg")
                .credential(testCredential)
                .build();

        testCredential.setUser(testUser);

        CredentialDto credentialDto = CredentialDto.builder()
                .username("testuser")
                .password("testpassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();

        testUserDto = UserDto.builder()
                .firstName("Test")
                .lastName("User")
                .email("testuser@example.com")
                .phone("1234567890")
                .imageUrl("http://example.com/image.jpg")
                .credentialDto(credentialDto)
                .build();
    }    
    
    @Test
    @Order(1)
    @DisplayName("Integration Test - GET /api/users - Should return empty list when no users")
    void testFindAllUsers_EmptyList() throws Exception {
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection", hasSize(0)));
    }    
    
    @Test
    @Order(2)
    @DisplayName("Integration Test - POST /api/users - Should create new user successfully")
    void testCreateUser_Success() throws Exception {
        String userJson = objectMapper.writeValueAsString(testUserDto);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.email", is("testuser@example.com")))
                .andExpect(jsonPath("$.phone", is("1234567890")))
                .andExpect(jsonPath("$.credential.username", is("testuser")))
                .andExpect(jsonPath("$.userId", notNullValue()));

        List<User> users = userRepository.findAll();
        assert users.size() == 1;
        assert users.get(0).getEmail().equals("testuser@example.com");
    }



    @Test
    @Order(3)
    @DisplayName("Integration Test - GET /api/users/{userId} - Should return user by ID")
    void testFindUserById_Success() throws Exception {
        User savedUser = userRepository.save(testUser);

        mockMvc.perform(get("/api/users/{userId}", savedUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())                .andExpect(jsonPath("$.userId", is(savedUser.getUserId())))
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.email", is("testuser@example.com")))
                .andExpect(jsonPath("$.credential.username", is("testuser")));
    }

    @Test
    @Order(4)
    @DisplayName("Integration Test - GET /api/users/{userId} - Should result in UserObjectNotFoundException for non-existent user")
    void testFindUserById_NotFound() throws Exception {
        mockMvc.perform(get("/api/users/{userId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg", is("#### User with id: 999 not found! ####")))
                .andExpect(jsonPath("$.httpStatus", is("BAD_REQUEST")));
    }

    @Test
    @Order(5)
    @DisplayName("Integration Test - GET /api/users/username/{username} - Should return user by username")
    void testFindUserByUsername_Success() throws Exception {        
        userRepository.save(testUser);

        mockMvc.perform(get("/api/users/username/{username}", "testuser")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Test")))
                .andExpect(jsonPath("$.lastName", is("User")))
                .andExpect(jsonPath("$.email", is("testuser@example.com")))
                .andExpect(jsonPath("$.credential.username", is("testuser")));
    }

    @Test
    @Order(6)
    @DisplayName("Integration Test - PUT /api/users/{userId} - Should update user with path variable")
    void testUpdateUserWithId_Success() throws Exception {
        User savedUser = userRepository.save(testUser);

        testUserDto.setUserId(savedUser.getUserId()); 
        testUserDto.setFirstName("Updated via Path");
        testUserDto.setEmail("pathupdate@example.com");
        if (testUserDto.getCredentialDto() == null) {
            testUserDto.setCredentialDto(CredentialDto.builder()
                .username("testuser")
                .password("testpassword")
                .roleBasedAuthority(RoleBasedAuthority.ROLE_USER)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build());
        }

        String userJson = objectMapper.writeValueAsString(testUserDto);

        mockMvc.perform(put("/api/users/{userId}", savedUser.getUserId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Updated via Path")))
                .andExpect(jsonPath("$.email", is("pathupdate@example.com")));
    }

    @Test
    @Order(7)
    @DisplayName("Integration Test - DELETE /api/users/{userId} - Should delete user successfully")
    void testDeleteUser_Success() throws Exception {
        User savedUser = userRepository.save(testUser);
        Integer userId = savedUser.getUserId();

        mockMvc.perform(delete("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(true)));

        assert !userRepository.existsById(userId);
    }

}