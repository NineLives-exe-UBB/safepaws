package com.ninelives.safepaws.config;

import com.ninelives.safepaws.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger UI and the OpenAPI document are public; the protected endpoints still need a token.
 * Runs the whole application with the real security rules and springdoc.
 *
 * <p>{@code TestSecurityConfig} adds a valid token to every request that has none, so each request
 * here sends an invalid token instead: the filter ignores it, which makes the request anonymous.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=test-jwt-secret-key-for-safepaws-automated-tests-32b",
        "app.cors.allowed-origins=http://localhost:4200",
        "app.frontend.base-url=http://localhost:4200"
})
class SwaggerAccessTest {

    private static final String ANONYMOUS = "Bearer not-a-valid-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs").header(HttpHeaders.AUTHORIZATION, ANONYMOUS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/announcements']").exists());
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html").header(HttpHeaders.AUTHORIZATION, ANONYMOUS))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiShortcutIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui.html").header(HttpHeaders.AUTHORIZATION, ANONYMOUS))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void protectedEndpointStillNeedsAToken() throws Exception {
        mockMvc.perform(post("/announcements").header(HttpHeaders.AUTHORIZATION, ANONYMOUS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
    }
}
