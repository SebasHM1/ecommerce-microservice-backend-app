package com.selimhorri.app.business.test.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.test.service.RetryTemplateService;

/**
 * Controlador que demuestra el uso de RetryTemplate
 * a través de un servicio dedicado
 */
@RestController
@RequestMapping("/api/test/retry-template")
@Slf4j
public class RetryTemplateController {

    @Autowired
    private RetryTemplateService retryTemplateService;

    /**
     * MÉTODO 5: RetryTemplate por defecto
     * - Configuración estándar con 3 intentos
     * - Backoff exponencial (1s, 2s, 4s)
     * - Fallback incluido
     */
    @GetMapping("/default/user/{userId}")
    public ResponseEntity<UserDto> getUserWithDefaultRetry(@PathVariable("userId") String userId) {
        log.info("=== Iniciando Default RetryTemplate para userId: {} ===", userId);
        
        try {
            UserDto user = retryTemplateService.getUserWithDefaultRetry(userId);
            log.info("Usuario obtenido con Default RetryTemplate: {}", user);
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Error final en Default RetryTemplate: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MÉTODO 6: RetryTemplate para servicios externos
     * - 4 intentos máximo
     * - Backoff más agresivo (0.5s, 1.25s, 3.125s, 7.8s)
     * - Excepciones específicas configuradas
     */
    @GetMapping("/external/user/{userId}")
    public ResponseEntity<UserDto> getUserWithExternalServiceRetry(@PathVariable("userId") String userId) {
        log.info("=== Iniciando External Service RetryTemplate para userId: {} ===", userId);
        
        try {
            UserDto user = retryTemplateService.getUserWithExternalServiceRetry(userId);
            log.info("Usuario obtenido con External Service RetryTemplate: {}", user);
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Error final en External Service RetryTemplate: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MÉTODO 7: RetryTemplate rápido
     * - 5 intentos con delays cortos
     * - Backoff rápido (200ms, 300ms, 450ms, 675ms, 1s)
     * - Para operaciones que deben ser rápidas
     */
    @GetMapping("/fast/user/{userId}")
    public ResponseEntity<UserDto> getUserWithFastRetry(@PathVariable("userId") String userId) {
        log.info("=== Iniciando Fast RetryTemplate para userId: {} ===", userId);
        
        try {
            UserDto user = retryTemplateService.getUserWithFastRetry(userId);
            log.info("Usuario obtenido con Fast RetryTemplate: {}", user);
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Error final en Fast RetryTemplate: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * MÉTODO 8: RetryTemplate sin fallback
     * - Lanza excepción si todos los intentos fallan
     * - Para casos donde el fallo debe propagarse
     */
    @GetMapping("/no-fallback/user/{userId}")
    public ResponseEntity<UserDto> getUserWithRetryNoFallback(@PathVariable("userId") String userId) {
        log.info("=== Iniciando No Fallback RetryTemplate para userId: {} ===", userId);
        
        try {
            UserDto user = retryTemplateService.getUserWithRetryNoFallback(userId);
            log.info("Usuario obtenido con No Fallback RetryTemplate: {}", user);
            return ResponseEntity.ok(user);
        } catch (Exception ex) {
            log.error("Error final sin fallback: {}", ex.getMessage());
            return ResponseEntity.status(503).body(null); // Service Unavailable
        }
    }

    /**
     * Endpoint de información sobre las configuraciones disponibles
     */
    @GetMapping("/info")
    public ResponseEntity<String> getRetryTemplateInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== RetryTemplate Configurations ===\n\n");
        
        info.append("1. DEFAULT RetryTemplate:\n");
        info.append("   - Intentos: 3\n");
        info.append("   - Backoff: Exponencial (1s, 2s, 4s)\n");
        info.append("   - Test ID: 'retry-default'\n");
        info.append("   - Endpoint: /api/test/retry-template/default/user/{userId}\n\n");
        
        info.append("2. EXTERNAL SERVICE RetryTemplate:\n");
        info.append("   - Intentos: 4\n");
        info.append("   - Backoff: Agresivo (0.5s, 1.25s, 3.125s, 7.8s)\n");
        info.append("   - Test ID: 'retry-external'\n");
        info.append("   - Endpoint: /api/test/retry-template/external/user/{userId}\n\n");
        
        info.append("3. FAST RetryTemplate:\n");
        info.append("   - Intentos: 5\n");
        info.append("   - Backoff: Rápido (200ms, 300ms, 450ms, 675ms, 1s)\n");
        info.append("   - Test ID: 'retry-fast'\n");
        info.append("   - Endpoint: /api/test/retry-template/fast/user/{userId}\n\n");
        
        info.append("4. NO FALLBACK RetryTemplate:\n");
        info.append("   - Intentos: 3\n");
        info.append("   - Sin fallback (lanza excepción)\n");
        info.append("   - Test ID: 'retry-no-fallback'\n");
        info.append("   - Endpoint: /api/test/retry-template/no-fallback/user/{userId}\n\n");
        
        info.append("Para probar fallas, usa los Test IDs mencionados como userId.\n");
        info.append("Para éxito, usa cualquier otro userId (ej: '1', '2', etc.)");
        
        return ResponseEntity.ok(info.toString());
    }

    /**
     * Endpoint para probar todas las configuraciones de una vez
     */
    @GetMapping("/test-all/{userId}")
    public ResponseEntity<String> testAllConfigurations(@PathVariable("userId") String userId) {
        StringBuilder results = new StringBuilder();
        results.append("=== Prueba de Todas las Configuraciones ===\n\n");
        
        // Test Default
        try {
            long startTime = System.currentTimeMillis();
            UserDto user = retryTemplateService.getUserWithDefaultRetry(userId);
            long duration = System.currentTimeMillis() - startTime;
            results.append("✅ DEFAULT: Éxito en ").append(duration).append("ms - ").append(user.getFirstName()).append("\n");
        } catch (Exception ex) {
            results.append("❌ DEFAULT: Error - ").append(ex.getMessage()).append("\n");
        }
        
        // Test External
        try {
            long startTime = System.currentTimeMillis();
            UserDto user = retryTemplateService.getUserWithExternalServiceRetry(userId);
            long duration = System.currentTimeMillis() - startTime;
            results.append("✅ EXTERNAL: Éxito en ").append(duration).append("ms - ").append(user.getFirstName()).append("\n");
        } catch (Exception ex) {
            results.append("❌ EXTERNAL: Error - ").append(ex.getMessage()).append("\n");
        }
        
        // Test Fast
        try {
            long startTime = System.currentTimeMillis();
            UserDto user = retryTemplateService.getUserWithFastRetry(userId);
            long duration = System.currentTimeMillis() - startTime;
            results.append("✅ FAST: Éxito en ").append(duration).append("ms - ").append(user.getFirstName()).append("\n");
        } catch (Exception ex) {
            results.append("❌ FAST: Error - ").append(ex.getMessage()).append("\n");
        }
        
        return ResponseEntity.ok(results.toString());
    }
}
