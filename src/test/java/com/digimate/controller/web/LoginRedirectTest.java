
package com.digimate.controller.web;

import com.digimate.config.SecurityConfig;
import com.digimate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * After a real form login everyone goes to "/", and the home page then sends each role to its own page.
 */
@WebMvcTest(AuthPageController.class)
@Import(SecurityConfig.class)
class LoginRedirectTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;

    @TestConfiguration
    static class TwoAccounts {
        @Bean
        UserDetailsService userDetailsService(PasswordEncoder encoder) {
            return new InMemoryUserDetailsManager(
                    User.withUsername("admin@example.test").password(encoder.encode("password-1")).roles("ADMIN").build(),
                    User.withUsername("anna@example.test").password(encoder.encode("password-1")).roles("USER").build());
        }
    }

    @Test
    void shouldSendAnAdminToTheHomePageAfterLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("admin@example.test").password("password-1"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldSendANormalUserToTheHomePageAfterLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("anna@example.test").password("password-1"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void shouldSendAWrongPasswordBackToTheLoginPage() throws Exception {
        mockMvc.perform(formLogin("/login").user("anna@example.test").password("wrong-password"))
                .andExpect(redirectedUrl("/login?error"));
    }
}