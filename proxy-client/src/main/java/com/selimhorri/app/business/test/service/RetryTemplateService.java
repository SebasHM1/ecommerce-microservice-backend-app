package com.selimhorri.app.business.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.business.user.model.UserDto;

import org.springframework.http.ResponseEntity;

/**
 * Servicio que demuestra el uso de RetryTemplate
 * para llamadas a servicios externos con diferentes estrategias de retry
 */
@Service
@Slf4j
public class RetryTemplateService {

    @Autowired
    private UserClientService userClientService;

    @Autowired
    @Qualifier("defaultRetryTemplate")
    private RetryTemplate defaultRetryTemplate;

    @Autowired
    @Qualifier("externalServiceRetryTemplate")
    private RetryTemplate externalServiceRetryTemplate;

    @Autowired
    @Qualifier("fastRetryTemplate")
    private RetryTemplate fastRetryTemplate;

    /**
     * Obtener usuario usando RetryTemplate por defecto
     */
    public UserDto getUserWithDefaultRetry(String userId) {
        log.info("Ejecutando getUserWithDefaultRetry para userId: {}", userId);
        
        return defaultRetryTemplate.execute(context -> {
            log.info("Intento {} de {} para getUserWithDefaultRetry", 
                    context.getRetryCount() + 1, 3);
            
            // Simular fallo para testing
            if ("retry-default".equals(userId)) {
                throw new RuntimeException("Error simulado para default retry");
            }
            
            ResponseEntity<UserDto> response = userClientService.findById(userId);
            return response.getBody();
        }, context -> {
            // Recovery callback (fallback)
            log.error("Fallback ejecutado para getUserWithDefaultRetry userId: {}", userId);
            return UserDto.builder()
                    .userId(Integer.parseInt(userId))
                    .firstName("Default Retry")
                    .lastName("Fallback")
                    .email("default-retry-fallback@example.com")
                    .build();
        });
    }

    /**
     * Obtener usuario usando RetryTemplate para servicios externos
     */
    public UserDto getUserWithExternalServiceRetry(String userId) {
        log.info("Ejecutando getUserWithExternalServiceRetry para userId: {}", userId);
        
        return externalServiceRetryTemplate.execute(context -> {
            log.info("Intento {} de {} para getUserWithExternalServiceRetry", 
                    context.getRetryCount() + 1, 4);
            
            // Simular fallo para testing
            if ("retry-external".equals(userId)) {
                throw new RuntimeException("Error simulado para external service retry");
            }
            
            ResponseEntity<UserDto> response = userClientService.findById(userId);
            return response.getBody();
        }, context -> {
            // Recovery callback
            log.error("Fallback ejecutado para getUserWithExternalServiceRetry userId: {}", userId);
            return UserDto.builder()
                    .userId(Integer.parseInt(userId))
                    .firstName("External Service")
                    .lastName("Fallback")
                    .email("external-service-fallback@example.com")
                    .build();
        });
    }

    /**
     * Obtener usuario usando RetryTemplate rápido
     */
    public UserDto getUserWithFastRetry(String userId) {
        log.info("Ejecutando getUserWithFastRetry para userId: {}", userId);
        
        return fastRetryTemplate.execute(context -> {
            log.info("Intento {} de {} para getUserWithFastRetry", 
                    context.getRetryCount() + 1, 5);
            
            // Simular fallo para testing
            if ("retry-fast".equals(userId)) {
                throw new RuntimeException("Error simulado para fast retry");
            }
            
            ResponseEntity<UserDto> response = userClientService.findById(userId);
            return response.getBody();
        }, context -> {
            // Recovery callback
            log.error("Fallback ejecutado para getUserWithFastRetry userId: {}", userId);
            return UserDto.builder()
                    .userId(Integer.parseInt(userId))
                    .firstName("Fast Retry")
                    .lastName("Fallback")
                    .email("fast-retry-fallback@example.com")
                    .build();
        });
    }

    /**
     * Método que demuestra retry sin recovery (lanza excepción si falla)
     */
    public UserDto getUserWithRetryNoFallback(String userId) {
        log.info("Ejecutando getUserWithRetryNoFallback para userId: {}", userId);
        
        return defaultRetryTemplate.execute(context -> {
            log.info("Intento {} para getUserWithRetryNoFallback", context.getRetryCount() + 1);
            
            if ("retry-no-fallback".equals(userId)) {
                throw new RuntimeException("Error que provocará excepción final");
            }
            
            ResponseEntity<UserDto> response = userClientService.findById(userId);
            return response.getBody();
        });
        // No recovery callback - lanzará excepción si todos los intentos fallan
    }
}
