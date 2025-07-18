package com.loki.bonos.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a todas las rutas
                .allowedOrigins("http://localhost:5173",
                        "https://maxikash-loki-front.vercel.app", 
                        "https://lokimaxi.firebaseapp.com/",
                        "https://lokimaxi.web.app/",
                        "https://loki.maxi-dev.net",
                        "https://loki.maxi-prod.net") // Permite solicitudes desde el frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Métodos permitidos
                .allowedHeaders("*") // Permite todos los encabezados
                .allowCredentials(true); // Permite el uso de cookies o credenciales
    }
}
