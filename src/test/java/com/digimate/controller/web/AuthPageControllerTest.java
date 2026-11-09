package com.digimate.controller.web;

import com.digimate.config.SecurityConfig;
import com.digimate.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthPageController.class)
@Import(SecurityConfig.class)
class AuthPageControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean UserService userService;

    @Test
    @WithMockUser
    void shouldSendALoggedInUserFromTheHomePageToMyRequests() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/requests/my"));
    }

    @Test
    @WithAnonymousUser
    void shouldSendAGuestFromTheHomePageToTheLoginPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
    @Test
    @WithAnonymousUser
    void shouldServeBootstrapAndOurStylesToAGuest() throws Exception {
        mockMvc.perform(get("/webjars/bootstrap/5.3.8/css/bootstrap.min.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSendAnAdminFromTheHomePageToTheHelpersPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/helpers"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRefuseANormalUserOnTheAdminPages() throws Exception {
        mockMvc.perform(get("/admin/helpers"))
                .andExpect(status().isForbidden());
    }
}