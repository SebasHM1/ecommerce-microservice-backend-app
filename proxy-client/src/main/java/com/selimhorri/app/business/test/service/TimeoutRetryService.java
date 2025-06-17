package com.selimhorri.app.business.test.service;

import com.selimhorri.app.business.user.service.UserClientService;
import com.selimhorri.app.business.user.model.UserDto;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Servicio que combina patrones Retry + Timeout para manejo de resilencia
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutRetryService {    private final UserClientService userClientService;
    private final Retry userServiceRetry;

    /**
     * Método que combina Retry + Timeout usando Resilience4j
     * Timeout se aplica POR CADA intento de retry
     */
    public UserDto getUserWithRetryAndTimeout(Integer userId) {
        log.info("🔄⏱️ Iniciando getUserWithRetryAndTimeout para userId: {}", userId);

        Supplier<UserDto> decoratedSupplier = Retry.decorateSupplier(userServiceRetry, () -> {
            log.info("⏱️ Ejecutando con timeout de 3s para userId: {}", userId);
            
            try {
                // Timeout manual usando Future
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<UserDto> future = executor.submit(() -> {
                    log.info("📞 Llamando al user-service para userId: {}", userId);
                    ResponseEntity<UserDto> response = userClientService.findById(userId.toString());
                    return response.getBody();
                });
                
                try {
                    UserDto result = future.get(3, TimeUnit.SECONDS);
                    executor.shutdown();
                    return result;
                } catch (TimeoutException e) {
                    future.cancel(true);
                    executor.shutdown();
                    log.warn("⏱️❌ Timeout de 3s alcanzado para userId: {}", userId);
                    throw new RuntimeException("Timeout en llamada individual", e);
                } catch (ExecutionException | InterruptedException e) {
                    executor.shutdown();
                    log.error("❌ Error en llamada para userId: {} - {}", userId, e.getMessage());
                    throw new RuntimeException("Error en llamada", e);
                }
                
            } catch (Exception e) {
                log.error("❌ Error general para userId: {} - {}", userId, e.getMessage());
                throw new RuntimeException("Error en llamada", e);
            }
        });

        try {
            UserDto result = decoratedSupplier.get();
            log.info("✅ Usuario obtenido exitosamente con timeout+retry: {}", result);
            return result;
        } catch (Exception e) {
            log.error("🚨 Todos los intentos con timeout fallaron para userId: {}, activando fallback", userId);
            return createTimeoutFallbackUser(userId, "Retry+Timeout");        }
    }

    /**
     * Timeout rápido (1 segundo) + Retry con Spring annotations
     */
    @Retryable(
        value = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 500)
    )
    public CompletableFuture<UserDto> getUserWithFastTimeout(Integer userId) {
        log.info("🔄⚡ Iniciando getUserWithFastTimeout (1s) para userId: {}", userId);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("⚡ Ejecutando con timeout rápido de 1s para userId: {}", userId);
                
                // Aplicamos timeout de 1 segundo manualmente
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<UserDto> future = executor.submit(() -> {
                    ResponseEntity<UserDto> response = userClientService.findById(userId.toString());
                    return response.getBody();
                });
                
                try {
                    UserDto result = future.get(1, TimeUnit.SECONDS);
                    executor.shutdown();
                    log.info("✅ Usuario obtenido con fast timeout: {}", result);
                    return result;
                } catch (TimeoutException e) {
                    future.cancel(true);
                    executor.shutdown();
                    log.warn("⚡❌ Fast timeout de 1s alcanzado para userId: {}", userId);
                    throw new RuntimeException("Fast timeout alcanzado", e);
                } catch (ExecutionException | InterruptedException e) {
                    executor.shutdown();
                    throw new RuntimeException("Error en fast timeout", e);
                }
                
            } catch (Exception e) {
                log.error("❌ Error en fast timeout para userId: {} - {}", userId, e.getMessage());
                throw new RuntimeException("Error en fast timeout", e);
            }
        });
    }

    /**
     * Timeout personalizado con RetryTemplate manual
     */
    public UserDto getUserWithCustomTimeout(Integer userId, long timeoutSeconds) {
        log.info("🔄🕐 Iniciando getUserWithCustomTimeout ({}s) para userId: {}", timeoutSeconds, userId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try {
            // Retry manual con timeout personalizado
            for (int attempt = 1; attempt <= 3; attempt++) {
                log.info("🔄 Intento {} de 3 con timeout de {}s para userId: {}", attempt, timeoutSeconds, userId);
                
                Future<UserDto> future = executor.submit(() -> {
                    try {
                        log.info("📞 Llamada con timeout personalizado para userId: {}", userId);
                        ResponseEntity<UserDto> response = userClientService.findById(userId.toString());
                        return response.getBody();
                    } catch (Exception e) {
                        log.error("❌ Error en llamada con timeout personalizado: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }
                });

                try {
                    UserDto result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                    log.info("✅ Usuario obtenido con timeout personalizado: {}", result);
                    return result;
                    
                } catch (TimeoutException e) {
                    log.warn("⏱️❌ Timeout de {}s alcanzado en intento {} para userId: {}", timeoutSeconds, attempt, userId);
                    future.cancel(true);
                    
                    if (attempt < 3) {
                        log.info("⏳ Esperando 1s antes del siguiente intento...");
                        Thread.sleep(1000);
                    }
                } catch (ExecutionException e) {
                    log.error("❌ Error de ejecución en intento {} para userId: {}: {}", attempt, userId, e.getCause().getMessage());
                    if (attempt < 3) {
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("🔄 Operación interrumpida en intento {} para userId: {}", attempt, userId);
                    break;
                }
            }
            
            log.error("🚨 Todos los intentos con timeout personalizado fallaron para userId: {}", userId);
            return createTimeoutFallbackUser(userId, "Custom-Timeout-" + timeoutSeconds + "s");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("🔄 Operación principal interrumpida para userId: {}", userId);
            return createTimeoutFallbackUser(userId, "Interrupted-Timeout");
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Fallback method para Spring @TimeLimiter
     */
    public CompletableFuture<UserDto> fastTimeoutFallback(Integer userId, Exception ex) {
        log.warn("🚨⚡ Fast timeout fallback activado para userId: {} - {}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(createTimeoutFallbackUser(userId, "Fast-Timeout-Fallback"));
    }

    /**
     * Recover method para Spring @Retryable
     */
    @Recover
    public CompletableFuture<UserDto> recoverFromFastTimeout(Exception ex, Integer userId) {
        log.warn("🚨🔄 Recover de fast timeout activado para userId: {} - {}", userId, ex.getMessage());
        return CompletableFuture.completedFuture(createTimeoutFallbackUser(userId, "Fast-Timeout-Recover"));
    }

    /**
     * Método utilitario para crear usuarios fallback
     */
    private UserDto createTimeoutFallbackUser(Integer userId, String timeoutType) {
        return UserDto.builder()
            .userId(userId)
            .firstName("Timeout " + timeoutType)
            .lastName("Fallback")
            .email("timeout-fallback-" + timeoutType.toLowerCase().replace(" ", "-") + "@example.com")
            .build();
    }
}
