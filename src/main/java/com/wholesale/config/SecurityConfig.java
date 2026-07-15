package com.wholesale.config;

import com.wholesale.service.AuthService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthService authService;

    public SecurityConfig(AuthService authService) {
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/google-sheets/status").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/", "/index.html", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // for H2 console
            .addFilterBefore(authFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public Filter authFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String path = request.getRequestURI();

                // Skip auth check for public endpoints
                if (path.startsWith("/api/auth/") || path.equals("/api/google-sheets/status")
                        || path.startsWith("/h2-console/")
                        || path.equals("/") || path.equals("/index.html")
                        || path.startsWith("/css/") || path.startsWith("/js/")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    var user = authService.validateToken(token);
                    if (user != null) {
                        // Set user info as request attributes
                        request.setAttribute("userId", user.getId());
                        request.setAttribute("userRole", user.getRole());
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Unauthorized\"}");
            }
        };
    }
}
