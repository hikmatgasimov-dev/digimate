package com.digimate.config;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two security chains:
 * 1. /api/**  : HTTP Basic, no session, no CSRF. This is what lets Swagger "Authorize" try POST, PUT and DELETE.
 * 2. the pages: form login with a session, CSRF stays on for browser forms.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                .anyRequest().authenticated());
        return http.build();
    }
    @Bean
    @Order(2)
    public SecurityFilterChain pageSecurityFilterChain(HttpSecurity http) throws Exception{

        http
        // CSRF stays on (default) for browser forms
                .authorizeHttpRequests(auth->auth
                        //error must be public, otherwise a 401 or 403 is turned into a redirect to /login
                        .requestMatchers("/login", "/register", "/error", "/css/**",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
        // Spring's own login page for now. Anil's login.html replaces it in Sprint 1.
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults());
                return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

