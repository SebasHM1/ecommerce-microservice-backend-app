package com.selimhorri.app.business.test.controller;

import com.selimhorri.app.business.test.service.TimeoutRetryService;
import com.selimhorri.app.business.user.model.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Controller para probar patrones Timeout + Retry
 */
@Slf4j
@RestController
@RequestMapping("/api/test/timeout-retry")
@RequiredArgsConstructor
public class TimeoutRetryController {

    private final TimeoutRetryService timeoutRetryService;

    /**
     * Endpoint para probar Timeout + Retry usando Resilience4j
     * Timeout de 3 segundos por cada intento de retry
     * 
     * IDs de prueba:
     * - 999: Simula error constante (activa retry + timeout + fallback)
     * - 998: Simula error intermitente
     * - Otros: Operación normal (si el servicio está disponible)
     */
    @GetMapping("/resilience4j/user/{userId}")
    public ResponseEntity<UserDto> getUserWithResilience4jTimeout(@PathVariable("userId") final Integer userId) {
        log.info("🔄⏱️ Iniciando test de Timeout+Retry con Resilience4j para userId: {}", userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            UserDto user = timeoutRetryService.getUserWithRetryAndTimeout(userId);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ Test completado en {}ms. Usuario: {}", duration, user);
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Test falló después de {}ms: {}", duration, e.getMessage());
            
            // Fallback del controlador
            UserDto fallbackUser = UserDto.builder()
                .userId(userId)
                .firstName("Controller Timeout")
                .lastName("Final Fallback")
                .email("controller-timeout-fallback@example.com")
                .build();
                
            return ResponseEntity.ok(fallbackUser);
        }
    }

    /**
     * Endpoint para probar Fast Timeout (1 segundo) + Retry
     */
    @GetMapping("/fast-timeout/user/{userId}")
    public ResponseEntity<UserDto> getUserWithFastTimeout(@PathVariable("userId") final Integer userId) {
        log.info("🔄⚡ Iniciando test de Fast Timeout (1s) para userId: {}", userId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            CompletableFuture<UserDto> futureUser = timeoutRetryService.getUserWithFastTimeout(userId);
            UserDto user = futureUser.get(); // Esperar el resultado
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ Fast timeout test completado en {}ms. Usuario: {}", duration, user);
            return ResponseEntity.ok(user);
            
        } catch (ExecutionException | InterruptedException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Fast timeout test falló después de {}ms: {}", duration, e.getMessage());
            
            UserDto fallbackUser = UserDto.builder()
                .userId(userId)
                .firstName("Fast Timeout")
                .lastName("Controller Fallback")
                .email("fast-timeout-controller-fallback@example.com")
                .build();
                
            return ResponseEntity.ok(fallbackUser);
        }
    }

    /**
     * Endpoint para probar Custom Timeout configurable
     */
    @GetMapping("/custom-timeout/{timeoutSeconds}/user/{userId}")
    public ResponseEntity<UserDto> getUserWithCustomTimeout(
            @PathVariable("timeoutSeconds") final Long timeoutSeconds,
            @PathVariable("userId") final Integer userId) {
        
        log.info("🔄🕐 Iniciando test de Custom Timeout ({}s) para userId: {}", timeoutSeconds, userId);
        
        // Validar timeout razonable
        if (timeoutSeconds < 1 || timeoutSeconds > 30) {
            log.warn("⚠️ Timeout de {}s no válido, usando 5s por defecto", timeoutSeconds);
            return getUserWithCustomTimeout(5L, userId);
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            UserDto user = timeoutRetryService.getUserWithCustomTimeout(userId, timeoutSeconds);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("✅ Custom timeout test completado en {}ms. Usuario: {}", duration, user);
            return ResponseEntity.ok(user);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ Custom timeout test falló después de {}ms: {}", duration, e.getMessage());
            
            UserDto fallbackUser = UserDto.builder()
                .userId(userId)
                .firstName("Custom Timeout " + timeoutSeconds + "s")
                .lastName("Controller Fallback")
                .email("custom-timeout-" + timeoutSeconds + "s-fallback@example.com")
                .build();
                
            return ResponseEntity.ok(fallbackUser);
        }
    }

    /**
     * Endpoint para probar diferentes timeouts en secuencia
     */
    @GetMapping("/comparison/user/{userId}")
    public ResponseEntity<String> compareTimeouts(@PathVariable("userId") final Integer userId) {
        log.info("🔄📊 Iniciando comparación de timeouts para userId: {}", userId);
        
        StringBuilder results = new StringBuilder();
        results.append("=== COMPARACIÓN DE TIMEOUTS ===\\n\\n");
        
        // Test 1: Fast timeout (1s)
        long start = System.currentTimeMillis();
        try {
            timeoutRetryService.getUserWithFastTimeout(userId).get();
            results.append("✅ Fast Timeout (1s): ").append(System.currentTimeMillis() - start).append("ms\\n");
        } catch (Exception e) {
            results.append("❌ Fast Timeout (1s): ").append(System.currentTimeMillis() - start).append("ms - ").append(e.getMessage()).append("\\n");
        }
        
        // Test 2: Custom timeout (3s)
        start = System.currentTimeMillis();
        try {
            timeoutRetryService.getUserWithCustomTimeout(userId, 3L);
            results.append("✅ Custom Timeout (3s): ").append(System.currentTimeMillis() - start).append("ms\\n");
        } catch (Exception e) {
            results.append("❌ Custom Timeout (3s): ").append(System.currentTimeMillis() - start).append("ms - ").append(e.getMessage()).append("\\n");
        }
        
        // Test 3: Resilience4j timeout (3s)
        start = System.currentTimeMillis();
        try {
            timeoutRetryService.getUserWithRetryAndTimeout(userId);
            results.append("✅ Resilience4j Timeout (3s): ").append(System.currentTimeMillis() - start).append("ms\\n");
        } catch (Exception e) {
            results.append("❌ Resilience4j Timeout (3s): ").append(System.currentTimeMillis() - start).append("ms - ").append(e.getMessage()).append("\\n");
        }
        
        results.append("\\n=== FIN COMPARACIÓN ===");
        
        return ResponseEntity.ok(results.toString());
    }
}
