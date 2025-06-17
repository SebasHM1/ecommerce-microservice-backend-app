package com.selimhorri.app.config;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import feign.FeignException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración personalizada para Spring Retry y Resilience4j
 * Proporciona beans de RetryTemplate y componentes de Resilience4j configurados para diferentes casos de uso
 */
@Configuration
public class RetryConfiguration {

    /**
     * RetryTemplate por defecto con configuración estándar
     */
    @Bean
    public RetryTemplate defaultRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Configurar política de retry
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Configurar backoff exponencial
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L); // 1 segundo
        backOffPolicy.setMultiplier(2.0); // Multiplicador exponencial
        backOffPolicy.setMaxInterval(10000L); // Máximo 10 segundos
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    /**
     * RetryTemplate específico para llamadas a servicios externos
     */
    @Bean
    public RetryTemplate externalServiceRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Definir excepciones que deben provocar retry
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(FeignException.class, true);
        retryableExceptions.put(FeignException.ServiceUnavailable.class, true);
        retryableExceptions.put(FeignException.InternalServerError.class, true);
        retryableExceptions.put(FeignException.BadGateway.class, true);
        retryableExceptions.put(FeignException.GatewayTimeout.class, true);
        retryableExceptions.put(java.net.ConnectException.class, true);
        retryableExceptions.put(java.net.SocketTimeoutException.class, true);
        
        // Excepciones que NO deben provocar retry
        retryableExceptions.put(FeignException.NotFound.class, false);
        retryableExceptions.put(FeignException.BadRequest.class, false);
        retryableExceptions.put(IllegalArgumentException.class, false);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(4, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Backoff más agresivo para servicios externos
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(500L);
        backOffPolicy.setMultiplier(2.5);
        backOffPolicy.setMaxInterval(15000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }    /**
     * RetryTemplate para operaciones rápidas con delay mínimo
     */
    @Bean
    public RetryTemplate fastRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Backoff más rápido
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(200L); // 200ms inicial
        backOffPolicy.setMultiplier(1.5);
        backOffPolicy.setMaxInterval(2000L); // Máximo 2 segundos
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }

    /**
     * Bean Resilience4j Retry para servicios de usuario
     */
    @Bean
    public Retry userServiceRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(1))
            .retryExceptions(Exception.class)
            .build();
            
        return Retry.of("userService", config);
    }

    /**
     * Bean Resilience4j TimeLimiter para timeout
     */
    @Bean
    public TimeLimiter userServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
            
        return TimeLimiter.of("userService", config);
    }

    /**
     * Bean Resilience4j TimeLimiter para timeout rápido
     */
    @Bean
    public TimeLimiter fastTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(2))
            .build();
            
        return TimeLimiter.of("fastTimeout", config);
    }
}
