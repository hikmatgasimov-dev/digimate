package com.digimate.controller.api;

import com.digimate.entity.User;
import com.digimate.entity.enums.HelperStatus;
import com.digimate.entity.enums.Role;
import com.digimate.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real login with a real token against the real security chain (database: digimate_test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtLoginIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminEmail;
    private String userEmail;

    @BeforeEach
    void createUsers() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        adminEmail = "admin-" + unique + "@example.test";
        userEmail = "user-" + unique + "@example.test";
        save(adminEmail, Role.ADMIN);
        save(userEmail, Role.USER);
    }

    private void save(String email, Role role) {
        User user = new User();
        user.setFullName("Test " + role);
        user.setEmail(email);
        user.setPhone("20123456");
        user.setPasswordHash(passwordEncoder.encode("secret-123"));
        user.setRole(role);
        user.setHelperStatus(HelperStatus.NONE);
        userRepository.save(user);
    }

    private String loginJson(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private String login(String email, String password) throws Exception {
        String body = loginJson(email, password);
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    @Test
    void shouldLetAnAdminUseTheTokenOnAnAdminEndpoint() throws Exception {
        String token = login(adminEmail, "secret-123");

        mockMvc.perform(get("/api/admin/helpers/pending").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn403WhenANormalUserCallsAnAdminEndpoint() throws Exception {
        String token = login(userEmail, "secret-123");

        mockMvc.perform(get("/api/admin/helpers/pending").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn401WhenThereIsNoToken() throws Exception {
        mockMvc.perform(get("/api/admin/helpers/pending")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenTheTokenIsBroken() throws Exception {
        mockMvc.perform(get("/api/admin/helpers/pending").header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenThePasswordIsWrong() throws Exception {
        String body = loginJson(userEmail, "wrong-password");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenTheEmailDoesNotExist() throws Exception {
        String body = loginJson("nobody@example.test", "secret-123");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
