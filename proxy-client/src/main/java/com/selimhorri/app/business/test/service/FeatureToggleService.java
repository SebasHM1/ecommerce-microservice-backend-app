package com.selimhorri.app.business.test.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para gestionar Feature Toggles (Feature Flags)
 * Permite activar/desactivar funcionalidades dinámicamente
 */
@Slf4j
@Service
public class FeatureToggleService {

    // Feature toggles estáticos desde configuración
    @Value("${feature.toggle.new-user-service:false}")
    private boolean newUserServiceEnabled;

    @Value("${feature.toggle.enhanced-logging:true}")
    private boolean enhancedLoggingEnabled;

    @Value("${feature.toggle.caching:false}")
    private boolean cachingEnabled;

    @Value("${feature.toggle.retry-fallback:true}")
    private boolean retryFallbackEnabled;

    // Feature toggles dinámicos (pueden cambiar en runtime)
    private final Map<String, Boolean> dynamicToggles = new ConcurrentHashMap<>();

    // Feature toggles basados en usuarios
    private final Map<String, Set<String>> userBasedToggles = new ConcurrentHashMap<>();

    // Feature toggles basados en porcentajes
    private final Map<String, Integer> percentageToggles = new ConcurrentHashMap<>();

    /**
     * Constructor - Inicializa algunos feature toggles dinámicos por defecto
     */
    public FeatureToggleService() {
        initializeDefaultToggles();
    }

    private void initializeDefaultToggles() {
        log.info("🚩 Inicializando Feature Toggles por defecto");
        
        // Toggles dinámicos
        dynamicToggles.put("experimental-endpoint", false);
        dynamicToggles.put("beta-features", false);
        dynamicToggles.put("maintenance-mode", false);
        dynamicToggles.put("premium-features", true);
        
        // Toggles basados en usuarios (usuarios beta)
        userBasedToggles.put("beta-users", new HashSet<>(Arrays.asList("user123", "user456", "admin")));
        userBasedToggles.put("vip-users", new HashSet<>(Arrays.asList("vip1", "vip2")));
        
        // Toggles basados en porcentajes
        percentageToggles.put("new-algorithm", 30); // 30% de usuarios
        percentageToggles.put("ui-redesign", 10);   // 10% de usuarios
        percentageToggles.put("performance-boost", 50); // 50% de usuarios
        
        log.info("✅ Feature Toggles inicializados: Dynamic={}, UserBased={}, Percentage={}", 
                 dynamicToggles.size(), userBasedToggles.size(), percentageToggles.size());
    }

    // ================================
    // STATIC FEATURE TOGGLES
    // ================================

    /**
     * Verifica si el nuevo servicio de usuarios está habilitado
     */
    public boolean isNewUserServiceEnabled() {
        boolean enabled = newUserServiceEnabled;
        log.debug("🚩 Feature Toggle 'new-user-service': {}", enabled);
        return enabled;
    }

    /**
     * Verifica si el logging mejorado está habilitado
     */
    public boolean isEnhancedLoggingEnabled() {
        boolean enabled = enhancedLoggingEnabled;
        if (enabled) {
            log.debug("🚩 Feature Toggle 'enhanced-logging': ENABLED");
        }
        return enabled;
    }

    /**
     * Verifica si el caching está habilitado
     */
    public boolean isCachingEnabled() {
        boolean enabled = cachingEnabled;
        log.debug("🚩 Feature Toggle 'caching': {}", enabled);
        return enabled;
    }

    /**
     * Verifica si el fallback de retry está habilitado
     */
    public boolean isRetryFallbackEnabled() {
        boolean enabled = retryFallbackEnabled;
        log.debug("🚩 Feature Toggle 'retry-fallback': {}", enabled);
        return enabled;
    }

    // ================================
    // DYNAMIC FEATURE TOGGLES
    // ================================

    /**
     * Verifica si un feature toggle dinámico está habilitado
     */
    public boolean isDynamicFeatureEnabled(String featureName) {
        boolean enabled = dynamicToggles.getOrDefault(featureName, false);
        log.debug("🚩 Dynamic Feature Toggle '{}': {}", featureName, enabled);
        return enabled;
    }

    /**
     * Activa un feature toggle dinámico
     */
    public void enableDynamicFeature(String featureName) {
        dynamicToggles.put(featureName, true);
        log.info("🟢 Feature Toggle '{}' ACTIVADO dinámicamente", featureName);
    }

    /**
     * Desactiva un feature toggle dinámico
     */
    public void disableDynamicFeature(String featureName) {
        dynamicToggles.put(featureName, false);
        log.info("🔴 Feature Toggle '{}' DESACTIVADO dinámicamente", featureName);
    }

    /**
     * Obtiene todos los feature toggles dinámicos
     */
    public Map<String, Boolean> getAllDynamicFeatures() {
        return new HashMap<>(dynamicToggles);
    }

    // ================================
    // USER-BASED FEATURE TOGGLES
    // ================================

    /**
     * Verifica si un usuario tiene acceso a una funcionalidad específica
     */
    public boolean isFeatureEnabledForUser(String featureName, String userId) {
        Set<String> enabledUsers = userBasedToggles.getOrDefault(featureName, Collections.emptySet());
        boolean enabled = enabledUsers.contains(userId);
        log.debug("🚩 User-based Feature Toggle '{}' for user '{}': {}", featureName, userId, enabled);
        return enabled;
    }

    /**
     * Agrega un usuario a un feature toggle
     */
    public void addUserToFeature(String featureName, String userId) {
        userBasedToggles.computeIfAbsent(featureName, k -> new HashSet<>()).add(userId);
        log.info("👤 Usuario '{}' agregado al Feature Toggle '{}'", userId, featureName);
    }

    /**
     * Remueve un usuario de un feature toggle
     */
    public void removeUserFromFeature(String featureName, String userId) {
        Set<String> users = userBasedToggles.get(featureName);
        if (users != null) {
            users.remove(userId);
            log.info("👤 Usuario '{}' removido del Feature Toggle '{}'", userId, featureName);
        }
    }

    // ================================
    // PERCENTAGE-BASED FEATURE TOGGLES
    // ================================

    /**
     * Verifica si un feature está habilitado basado en porcentaje para un usuario
     */
    public boolean isFeatureEnabledByPercentage(String featureName, String userId) {
        Integer percentage = percentageToggles.get(featureName);
        if (percentage == null) {
            log.debug("🚩 Percentage Feature Toggle '{}' not found", featureName);
            return false;
        }

        // Generar un hash consistente basado en el userId
        int userHash = Math.abs(userId.hashCode() % 100);
        boolean enabled = userHash < percentage;
        
        log.debug("🚩 Percentage Feature Toggle '{}' ({}%) for user '{}' (hash={}): {}", 
                 featureName, percentage, userId, userHash, enabled);
        return enabled;
    }

    /**
     * Actualiza el porcentaje de un feature toggle
     */
    public void updateFeaturePercentage(String featureName, int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("El porcentaje debe estar entre 0 y 100");
        }
        percentageToggles.put(featureName, percentage);
        log.info("📊 Feature Toggle '{}' actualizado a {}%", featureName, percentage);
    }

    // ================================
    // UTILITY METHODS
    // ================================

    /**
     * Obtiene el estado de todos los feature toggles para un usuario específico
     */
    public Map<String, Object> getFeatureStatusForUser(String userId) {
        Map<String, Object> status = new HashMap<>();
        
        // Static features
        Map<String, Boolean> staticFeatures = new HashMap<>();
        staticFeatures.put("new-user-service", isNewUserServiceEnabled());
        staticFeatures.put("enhanced-logging", isEnhancedLoggingEnabled());
        staticFeatures.put("caching", isCachingEnabled());
        staticFeatures.put("retry-fallback", isRetryFallbackEnabled());
        status.put("static", staticFeatures);
        
        // Dynamic features
        status.put("dynamic", getAllDynamicFeatures());
        
        // User-based features
        Map<String, Boolean> userFeatures = new HashMap<>();
        for (String feature : userBasedToggles.keySet()) {
            userFeatures.put(feature, isFeatureEnabledForUser(feature, userId));
        }
        status.put("userBased", userFeatures);
        
        // Percentage-based features
        Map<String, Boolean> percentageFeatures = new HashMap<>();
        for (String feature : percentageToggles.keySet()) {
            percentageFeatures.put(feature, isFeatureEnabledByPercentage(feature, userId));
        }
        status.put("percentage", percentageFeatures);
        
        return status;
    }

    /**
     * Verifica si el modo mantenimiento está activo
     */
    public boolean isMaintenanceModeEnabled() {
        return isDynamicFeatureEnabled("maintenance-mode");
    }

    /**
     * Activa el modo mantenimiento
     */
    public void enableMaintenanceMode() {
        enableDynamicFeature("maintenance-mode");
        log.warn("🚧 MODO MANTENIMIENTO ACTIVADO");
    }

    /**
     * Desactiva el modo mantenimiento
     */
    public void disableMaintenanceMode() {
        disableDynamicFeature("maintenance-mode");
        log.info("✅ Modo mantenimiento desactivado");
    }
}
