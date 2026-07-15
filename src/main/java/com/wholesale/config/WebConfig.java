package com.wholesale.config;

import com.wholesale.repository.UserRepository;
import com.wholesale.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    /**
     * Creates default users (admin/admin123, partner/partner123) on first startup.
     */
    @Bean
    public CommandLineRunner initData(UserRepository userRepository, AuthService authService) {
        return args -> {
            authService.createDefaultUsers();
        };
    }
}
