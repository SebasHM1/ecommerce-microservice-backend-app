package com.selimhorri.app.business.test.controller;

import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.business.user.model.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.*;

/**
 * Controller para probar Spring Retry con anotaciones @Retryable
 */
@Slf4j
@RestController
@RequestMapping("/api/test/spring-retry")
@RequiredArgsConstructor
public class SpringRetryController {

    private final UserClientService userClientService;

    /**
     * Endpoint para probar @Retryable con configuración por anotaciones
     * IDs de prueba:
     * - 999: Simula error constante (activa retry y fallback)
     * - 998: Simula error intermitente (retry exitoso)
     * - Otros: Operación normal
     */
    @GetMapping("/user/{userId}")
    @Retryable(
        value = {Exception.class}, 
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 1000)
    )
    public ResponseEntity<UserDto> getUserWithSpringRetry(@PathVariable("userId") final Integer userId) {
        log.info("🔄 Intentando obtener usuario con Spring @Retryable, userId: {}", userId);
          try {
            ResponseEntity<UserDto> response = userClientService.findById(userId.toString());
            UserDto user = response.getBody();
            log.info("✅ Usuario obtenido exitosamente con Spring @Retryable: {}", user);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("❌ Error en Spring @Retryable para userId {}: {}", userId, e.getMessage());
            throw e; // Re-lanza la excepción para que Spring Retry la capture
        }
    }

    /**
     * Método de recovery (fallback) para cuando fallan todos los intentos de retry
     */
    @Recover
    public ResponseEntity<UserDto> recoverFromUserServiceError(Exception ex, Integer userId) {
        log.warn("🚨 Todos los intentos fallaron para userId: {}, activando fallback. Error: {}", userId, ex.getMessage());
        
        UserDto fallbackUser = UserDto.builder()
            .userId(userId)
            .firstName("Spring Retry")
            .lastName("Fallback")
            .email("spring-retry-fallback@example.com")
            .build();
            
        log.info("🔄 Retornando usuario fallback: {}", fallbackUser);
        return ResponseEntity.ok(fallbackUser);
    }
}
