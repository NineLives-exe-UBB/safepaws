package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.config.JwtAuthenticationFilter;
import com.ninelives.safepaws.config.SecurityConfig;
import com.ninelives.safepaws.dto.LoginResponse;
import com.ninelives.safepaws.dto.RegisterRequest;
import com.ninelives.safepaws.dto.UserResponse;
import com.ninelives.safepaws.exception.EmailAlreadyRegisteredException;
import com.ninelives.safepaws.exception.InvalidCredentialsException;
import com.ninelives.safepaws.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ninelives.safepaws.service.AuthService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;


    private static final String VALID_BODY = """
            { "email": " Ana@SafePaws.dev ", "password": "SafePaws1!", "displayName": "Ana Pop" }
            """;

    @Test
    void registerReturns201WithTheUserAndNoPassword() throws Exception {
        when(authService.register(any())).thenReturn(new UserResponse(1L, "ana@safepaws.dev", "Ana Pop"));

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@safepaws.dev"))
                .andExpect(jsonPath("$.displayName").value("Ana Pop"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(authService).register(new RegisterRequest("ana@safepaws.dev", "SafePaws1!", "Ana Pop"));
    }

    @Test
    void missingFieldsReturn400WithOneMessageEach() throws Exception {
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(3)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("displayName"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Display name is required"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("email"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value("Email is required"))
                .andExpect(jsonPath("$.fieldErrors[2].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[2].message").value("Password is required"));

        verify(authService, never()).register(any());
    }

    @Test
    void invalidEmailAndWeakPasswordReturn400() throws Exception {
        String body = """
                { "email": "ana@", "password": "password", "displayName": "Ana" }
                """;

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Email must be a valid email address"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[1].message").value(
                        "Password must have at least 8 characters, one uppercase letter, one digit "
                                + "and one special character"));
    }

    @Test
    void takenEmailReturns409() throws Exception {
        when(authService.register(any())).thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void loginReturnsTokenAndUserWhenCredentialsAreValid() throws Exception {
        LocalDateTime expiresAt = LocalDateTime.of(2026, 11, 16, 14, 30, 0);
        UserResponse user = new UserResponse(1L, "ana@safepaws.dev", "Ana Pop");
        when(authService.login(any())).thenReturn(new LoginResponse("jwt-token", expiresAt, user));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@safepaws.dev",
                                  "password": "SafePaws1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expiresAt").value("2026-11-16T14:30:00"))
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.email").value("ana@safepaws.dev"))
                .andExpect(jsonPath("$.user.displayName").value("Ana Pop"));
    }
    @Test
    void loginReturns401WhenCredentialsAreInvalid() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ana@safepaws.dev",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
    @Test
    void meReturnsUserWhenTokenIsValid() throws Exception {
        String token = jwtService.generateToken(1L).token();
        when(authService.getCurrentUser(1L))
                .thenReturn(new UserResponse(1L, "ana@safepaws.dev", "Ana Pop"));

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@safepaws.dev"))
                .andExpect(jsonPath("$.displayName").value("Ana Pop"));
    }

    @Test
    void meReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }
}
