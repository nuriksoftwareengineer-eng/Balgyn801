package com.nurba.java;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that CdekWebhookController enforces X-Authorization token verification
 * when cdek.webhook-token is configured.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "cdek.webhook-token=test-cdek-secret-token"
})
class CdekWebhookAuthIntegrationTest {

    private static final String WEBHOOK_URL = "/api/v1/delivery/cdek/webhook";
    private static final String VALID_TOKEN  = "test-cdek-secret-token";

    private static final String PAYLOAD = """
            {
              "type": "ORDER",
              "uuid": "non-existent-uuid-12345",
              "attributes": {
                "cdek_number": "TEST-001",
                "status_code": "DELIVERED"
              }
            }
            """;

    @Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void webhook_withValidToken_returns200() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authorization", VALID_TOKEN)
                        .content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void webhook_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Authorization", "wrong-token")
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhook_withMissingToken_returns401() throws Exception {
        mockMvc.perform(post(WEBHOOK_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }
}
