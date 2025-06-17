package com.selimhorri.app.config;

import com.selimhorri.app.config.interceptor.FeatureToggleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración para registrar interceptors de Feature Toggles
 */
@Configuration
@RequiredArgsConstructor
public class FeatureToggleWebConfig implements WebMvcConfigurer {

    private final FeatureToggleInterceptor featureToggleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureToggleInterceptor)
                .addPathPatterns("/api/**")  // Aplicar a todos los endpoints de API
                .excludePathPatterns(
                        "/actuator/**",           // Excluir actuator
                        "/error",                 // Excluir página de error
                        "/favicon.ico"            // Excluir favicon
                );
    }
}
