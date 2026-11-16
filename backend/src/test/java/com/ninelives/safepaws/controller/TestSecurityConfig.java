package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.config.JwtAuthenticationFilter;
import com.ninelives.safepaws.config.SecurityConfig;
import com.ninelives.safepaws.service.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.WebApplicationContext;

@Configuration
@Import({SecurityConfig.class, JwtService.class})
public class TestSecurityConfig {

    @Bean
    MockMvcBuilderCustomizer defaultAuthHeader(JwtService jwtService) {
        String defaultToken = jwtService.generateToken(1L).token();
        return builder -> builder.apply(new MockMvcConfigurer() {
            @Override
            public RequestPostProcessor beforeMockMvcCreated(
                    ConfigurableMockMvcBuilder<?> b, WebApplicationContext context) {
                return request -> {
                    if (request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
                        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + defaultToken);
                    }
                    return request;
                };
            }
        });
    }
}