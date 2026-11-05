package com.digimate.config;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Import;
/**
 * Two security chains:
 /api/**  : login with a JWT token (Bearer), no session, no CSRF.
 *    The user calls POST /api/auth/login once, then sends "Authorization: Bearer
 */
@Configuration
@EnableWebSecurity
@Import(JwtConfig.class)
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .authorizeHttpRequests(auth ->

                        auth.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
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
    // The token carries the roles in the claim "roles" (already named ROLE_USER or ROLE_ADMIN).
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

}

