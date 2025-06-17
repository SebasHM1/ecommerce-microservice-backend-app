package com.selimhorri.app.business.test.controller;

import com.selimhorri.app.business.test.service.FeatureToggleService;
import com.selimhorri.app.business.user.model.UserDto;
import com.selimhorri.app.business.user.service.UserClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para gestionar y probar Feature Toggles
 */
@Slf4j
@RestController
@RequestMapping("/api/test/feature-toggle")
@RequiredArgsConstructor
public class FeatureToggleController {

    private final FeatureToggleService featureToggleService;
    private final UserClientService userClientService;

    // ================================
    // ENDPOINTS DE CONSULTA
    // ================================

    /**
     * Obtiene el estado de todos los feature toggles para un usuario
     */
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getFeatureStatus(@PathVariable String userId) {
        log.info("🚩 Consultando estado de Feature Toggles para usuario: {}", userId);
        
        Map<String, Object> status = featureToggleService.getFeatureStatusForUser(userId);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Obtiene todos los feature toggles dinámicos
     */
    @GetMapping("/dynamic")
    public ResponseEntity<Map<String, Boolean>> getDynamicFeatures() {
        log.info("🚩 Consultando Feature Toggles dinámicos");
        return ResponseEntity.ok(featureToggleService.getAllDynamicFeatures());
    }

    // ================================
    // ENDPOINTS DE GESTIÓN DINÁMICA
    // ================================

    /**
     * Activa un feature toggle dinámico
     */
    @PostMapping("/dynamic/{featureName}/enable")
    public ResponseEntity<Map<String, String>> enableDynamicFeature(@PathVariable String featureName) {
        log.info("🟢 Activando Feature Toggle dinámico: {}", featureName);
        
        featureToggleService.enableDynamicFeature(featureName);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Feature '" + featureName + "' activado exitosamente");
        response.put("feature", featureName);
        response.put("status", "enabled");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Desactiva un feature toggle dinámico
     */
    @PostMapping("/dynamic/{featureName}/disable")
    public ResponseEntity<Map<String, String>> disableDynamicFeature(@PathVariable String featureName) {
        log.info("🔴 Desactivando Feature Toggle dinámico: {}", featureName);
        
        featureToggleService.disableDynamicFeature(featureName);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Feature '" + featureName + "' desactivado exitosamente");
        response.put("feature", featureName);
        response.put("status", "disabled");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Actualiza el porcentaje de un feature toggle
     */
    @PostMapping("/percentage/{featureName}/{percentage}")
    public ResponseEntity<Map<String, Object>> updateFeaturePercentage(
            @PathVariable String featureName, 
            @PathVariable int percentage) {
        
        log.info("📊 Actualizando Feature Toggle '{}' a {}%", featureName, percentage);
        
        try {
            featureToggleService.updateFeaturePercentage(featureName, percentage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Porcentaje actualizado exitosamente");
            response.put("feature", featureName);
            response.put("percentage", percentage);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            response.put("feature", featureName);
            response.put("percentage", percentage);
            
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Agrega un usuario a un feature toggle
     */
    @PostMapping("/user/{featureName}/add/{userId}")
    public ResponseEntity<Map<String, String>> addUserToFeature(
            @PathVariable String featureName, 
            @PathVariable String userId) {
        
        log.info("👤 Agregando usuario '{}' al Feature Toggle '{}'", userId, featureName);
        
        featureToggleService.addUserToFeature(featureName, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Usuario agregado exitosamente al feature");
        response.put("feature", featureName);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Remueve un usuario de un feature toggle
     */
    @PostMapping("/user/{featureName}/remove/{userId}")
    public ResponseEntity<Map<String, String>> removeUserFromFeature(
            @PathVariable String featureName, 
            @PathVariable String userId) {
        
        log.info("👤 Removiendo usuario '{}' del Feature Toggle '{}'", userId, featureName);
        
        featureToggleService.removeUserFromFeature(featureName, userId);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Usuario removido exitosamente del feature");
        response.put("feature", featureName);
        response.put("userId", userId);
        
        return ResponseEntity.ok(response);
    }

    // ================================
    // ENDPOINTS DE PRUEBA CON FEATURE TOGGLES
    // ================================

    /**
     * Endpoint que usa feature toggles para decidir qué lógica ejecutar
     */
    @GetMapping("/demo/user/{userId}")
    public ResponseEntity<Map<String, Object>> getDemoUserWithFeatures(@PathVariable String userId) {
        log.info("🚩 Demo de Feature Toggles para usuario: {}", userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        
        // Feature Toggle Estático: Nuevo servicio de usuarios
        if (featureToggleService.isNewUserServiceEnabled()) {
            log.info("🚩 NEW USER SERVICE habilitado - usando nueva lógica");
            response.put("userService", "NEW_VERSION");
            response.put("features", "enhanced,fast,secure");
        } else {
            log.info("🚩 NEW USER SERVICE deshabilitado - usando lógica legacy");
            response.put("userService", "LEGACY_VERSION");
            response.put("features", "basic");
        }
        
        // Feature Toggle Dinámico: Funcionalidades premium
        if (featureToggleService.isDynamicFeatureEnabled("premium-features")) {
            log.info("🚩 PREMIUM FEATURES habilitado");
            response.put("premiumAccess", true);
            response.put("premiumFeatures", new String[]{"analytics", "reports", "priority_support"});
        } else {
            response.put("premiumAccess", false);
        }
        
        // Feature Toggle basado en Usuario: Beta features
        if (featureToggleService.isFeatureEnabledForUser("beta-users", userId)) {
            log.info("🚩 BETA FEATURES habilitado para usuario: {}", userId);
            response.put("betaAccess", true);
            response.put("betaFeatures", new String[]{"new_ui", "experimental_api", "advanced_search"});
        } else {
            response.put("betaAccess", false);
        }
        
        // Feature Toggle basado en Porcentaje: Nuevo algoritmo
        if (featureToggleService.isFeatureEnabledByPercentage("new-algorithm", userId)) {
            log.info("🚩 NEW ALGORITHM habilitado para usuario: {} (percentage-based)", userId);
            response.put("algorithmVersion", "v2.0_optimized");
            response.put("performanceBoost", "+30%");
        } else {
            response.put("algorithmVersion", "v1.0_standard");
        }
        
        // Enhanced Logging
        if (featureToggleService.isEnhancedLoggingEnabled()) {
            log.info("🚩 ENHANCED LOGGING habilitado - logs detallados activados");
            response.put("loggingLevel", "ENHANCED");
        } else {
            response.put("loggingLevel", "STANDARD");
        }
        
        // Modo Mantenimiento
        if (featureToggleService.isMaintenanceModeEnabled()) {
            log.warn("🚧 MODO MANTENIMIENTO activo");
            response.put("maintenanceMode", true);
            response.put("message", "Sistema en mantenimiento - funcionalidad limitada");
        } else {
            response.put("maintenanceMode", false);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint que simula llamada a servicio con fallback usando feature toggle
     */
    @GetMapping("/demo/user-service/{userId}")
    public ResponseEntity<UserDto> getUserWithFeatureToggle(@PathVariable String userId) {
        log.info("🚩 Obteniendo usuario con Feature Toggles: {}", userId);
        
        try {
            // Verificar si el modo mantenimiento está activo
            if (featureToggleService.isMaintenanceModeEnabled()) {
                log.warn("🚧 MODO MANTENIMIENTO - retornando respuesta limitada");
                return ResponseEntity.ok(createMaintenanceUser(userId));
            }
            
            // Verificar si el nuevo servicio está habilitado
            if (featureToggleService.isNewUserServiceEnabled()) {
                log.info("🚩 Usando NUEVO servicio de usuarios");
                // Aquí iría la lógica del nuevo servicio
                return ResponseEntity.ok(createEnhancedUser(userId));
            } else {
                log.info("🚩 Usando servicio LEGACY de usuarios");
                // Intentar usar el servicio legacy
                try {
                    ResponseEntity<UserDto> response = userClientService.findById(userId);
                    if (response.getBody() != null) {
                        return response;
                    }
                } catch (Exception e) {
                    log.warn("❌ Servicio legacy falló: {}", e.getMessage());
                }
                
                // Verificar si el fallback de retry está habilitado
                if (featureToggleService.isRetryFallbackEnabled()) {
                    log.info("🚩 RETRY FALLBACK habilitado - retornando usuario fallback");
                    return ResponseEntity.ok(createFallbackUser(userId));
                } else {
                    log.error("🚩 RETRY FALLBACK deshabilitado - error sin fallback");
                    throw new RuntimeException("Servicio no disponible y fallback deshabilitado");
                }
            }
        } catch (Exception e) {
            log.error("❌ Error en getUserWithFeatureToggle: {}", e.getMessage());
            // Siempre retornar algo, independientemente de los feature toggles
            return ResponseEntity.ok(createErrorUser(userId));
        }
    }

    // ================================
    // ENDPOINTS DE CONTROL DE MANTENIMIENTO
    // ================================

    /**
     * Activa el modo mantenimiento
     */
    @PostMapping("/maintenance/enable")
    public ResponseEntity<Map<String, String>> enableMaintenanceMode() {
        log.warn("🚧 Activando MODO MANTENIMIENTO");
        
        featureToggleService.enableMaintenanceMode();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Modo mantenimiento activado");
        response.put("status", "maintenance_enabled");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Desactiva el modo mantenimiento
     */
    @PostMapping("/maintenance/disable")
    public ResponseEntity<Map<String, String>> disableMaintenanceMode() {
        log.info("✅ Desactivando modo mantenimiento");
        
        featureToggleService.disableMaintenanceMode();
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Modo mantenimiento desactivado");
        response.put("status", "maintenance_disabled");
        
        return ResponseEntity.ok(response);
    }

    // ================================
    // MÉTODOS UTILITARIOS
    // ================================

    private UserDto createEnhancedUser(String userId) {
        return UserDto.builder()
                .userId(Integer.parseInt(userId))
                .firstName("Enhanced")
                .lastName("User v2.0")
                .email("enhanced-" + userId + "@newservice.com")
                .build();
    }

    private UserDto createFallbackUser(String userId) {
        return UserDto.builder()
                .userId(Integer.parseInt(userId))
                .firstName("Fallback")
                .lastName("User")
                .email("fallback-" + userId + "@service.com")
                .build();
    }

    private UserDto createMaintenanceUser(String userId) {
        return UserDto.builder()
                .userId(Integer.parseInt(userId))
                .firstName("Maintenance")
                .lastName("Mode")
                .email("maintenance-" + userId + "@service.com")
                .build();
    }

    private UserDto createErrorUser(String userId) {
        return UserDto.builder()
                .userId(Integer.parseInt(userId))
                .firstName("Error")
                .lastName("Recovery")
                .email("error-" + userId + "@service.com")
                .build();
    }
}
