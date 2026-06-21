package com.nurba.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurba.java.repositories.AppUserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRevocationIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final String REGISTER_URL      = "/api/v1/auth/register";
    private static final String REFRESH_COOKIE_URL = "/api/v1/auth/refresh-cookie";
    private static final String REFRESH_URL        = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL         = "/api/v1/auth/logout";
    private static final String COOKIE_NAME        = "refresh_token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        appUserRepository.deleteAll();
    }

    /** Cookie-based flow: logout revokes the token; old cookie is rejected. */
    @Test
    void logout_cookie_flow_revokesRefreshToken() throws Exception {
        // 1. Register → capture refresh_token cookie
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rev-cookie@test.com","password":"Password1!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie(COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        String tokenValue = refreshCookie.getValue();
        assertThat(tokenValue).isNotBlank();

        // 2. Refresh before logout → 200
        mockMvc.perform(post(REFRESH_COOKIE_URL).cookie(refreshCookie))
                .andExpect(status().isOk());

        // 3. Logout with the cookie — should revoke token
        mockMvc.perform(post(LOGOUT_URL).cookie(refreshCookie))
                .andExpect(status().isOk());

        // 4. Refresh with the OLD cookie value → 400 (token revoked)
        mockMvc.perform(post(REFRESH_COOKIE_URL).cookie(new Cookie(COOKIE_NAME, tokenValue)))
                .andExpect(status().isBadRequest());
    }

    /** Body-based flow: refresh endpoint also rejects revoked tokens. */
    @Test
    void logout_body_flow_revokesRefreshToken() throws Exception {
        // 1. Register → capture refresh_token cookie
        MvcResult registerResult = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"rev-body@test.com","password":"Password1!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie(COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        String tokenValue = refreshCookie.getValue();

        // 2. POST /auth/refresh with token in body → 200
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokenValue + "\"}"))
                .andExpect(status().isOk());

        // 3. Logout
        mockMvc.perform(post(LOGOUT_URL).cookie(refreshCookie))
                .andExpect(status().isOk());

        // 4. POST /auth/refresh with old token → 400
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokenValue + "\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Logout without a cookie (e.g. token already expired) still returns 200. */
    @Test
    void logout_withoutCookie_returns200() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isOk());
    }
}
