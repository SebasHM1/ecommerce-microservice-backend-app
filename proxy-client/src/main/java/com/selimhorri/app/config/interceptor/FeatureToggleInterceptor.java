package com.selimhorri.app.config.interceptor;

import com.selimhorri.app.business.test.service.FeatureToggleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Interceptor que usa Feature Toggles para controlar el flujo de requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureToggleInterceptor implements HandlerInterceptor {

    private final FeatureToggleService featureToggleService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Enhanced Logging
        if (featureToggleService.isEnhancedLoggingEnabled()) {
            log.info("🚩 [ENHANCED LOG] {} {} - Headers: {}, RemoteAddr: {}", 
                    method, requestURI, getHeadersInfo(request), request.getRemoteAddr());
        }
        
        // Verificar modo mantenimiento
        if (featureToggleService.isMaintenanceModeEnabled()) {
            // Permitir endpoints de gestión de feature toggles
            if (requestURI.contains("/feature-toggle/maintenance")) {
                log.info("🚩 Modo mantenimiento - permitiendo acceso a endpoint de gestión");
                return true;
            }
            
            // Permitir actuator para monitoreo
            if (requestURI.contains("/actuator")) {
                return true;
            }
            
            log.warn("🚧 MODO MANTENIMIENTO ACTIVO - bloqueando request: {} {}", method, requestURI);
            sendMaintenanceResponse(response);
            return false;
        }
        
        // Verificar feature toggles para endpoints específicos
        if (requestURI.contains("/api/test/feature-toggle")) {
            // Los endpoints de feature toggle siempre están disponibles
            return true;
        }
        
        // Verificar si endpoints experimentales están habilitados
        if (requestURI.contains("/experimental") || requestURI.contains("/beta")) {
            if (!featureToggleService.isDynamicFeatureEnabled("experimental-endpoint")) {
                log.warn("🚩 Endpoint experimental deshabilitado: {} {}", method, requestURI);
                sendFeatureDisabledResponse(response, "experimental-endpoint");
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (featureToggleService.isEnhancedLoggingEnabled()) {
            String requestURI = request.getRequestURI();
            String method = request.getMethod();
            int status = response.getStatus();
            
            if (ex != null) {
                log.error("🚩 [ENHANCED LOG] {} {} - Status: {} - Error: {}", 
                         method, requestURI, status, ex.getMessage());
            } else {
                log.info("🚩 [ENHANCED LOG] {} {} - Status: {} - Completed", 
                        method, requestURI, status);
            }
        }
    }

    private String getHeadersInfo(HttpServletRequest request) {
        StringBuilder headers = new StringBuilder();
        request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            headers.append(headerName).append("=").append(request.getHeader(headerName)).append("; ");
        });
        return headers.toString();
    }    private void sendMaintenanceResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType("application/json");
        String jsonResponse = "{\n" +
                "  \"error\": \"SERVICE_UNAVAILABLE\",\n" +
                "  \"message\": \"Sistema en modo mantenimiento\",\n" +
                "  \"status\": 503,\n" +
                "  \"timestamp\": \"" + java.time.Instant.now().toString() + "\"\n" +
                "}";
        response.getWriter().write(jsonResponse);
    }

    private void sendFeatureDisabledResponse(HttpServletResponse response, String feature) throws IOException {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType("application/json");
        String jsonResponse = "{\n" +
                "  \"error\": \"FEATURE_DISABLED\",\n" +
                "  \"message\": \"La funcionalidad '" + feature + "' está deshabilitada\",\n" +
                "  \"feature\": \"" + feature + "\",\n" +
                "  \"status\": 404,\n" +
                "  \"timestamp\": \"" + java.time.Instant.now().toString() + "\"\n" +
                "}";
        response.getWriter().write(jsonResponse);
    }
}
