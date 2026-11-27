package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.SightingResponse;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.service.SightingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SightingController.class)
@Import(TestSecurityConfig.class)
class SightingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SightingService sightingService;

    private static final String VALID_REQUEST = """
            {
              "latitude": 46.77,
              "longitude": 23.62,
              "seenAt": "2026-11-25T14:30:00",
              "note": "Near the park"
            }
            """;

    private SightingResponse response() {
        return new SightingResponse(
                5L,
                46.77,
                23.62,
                LocalDateTime.of(2026, 11, 25, 14, 30),
                "Near the park",
                new SightingResponse.Reporter(2L, "Test Reporter"),
                LocalDateTime.of(2026, 11, 25, 15, 0)
        );
    }

    @Test
    void createReturns201() throws Exception {
        when(sightingService.create(eq(10L), any()))
                .thenReturn(response());

        mockMvc.perform(post("/announcements/10/sightings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.latitude").value(46.77))
                .andExpect(jsonPath("$.reporter.id").value(2))
                .andExpect(jsonPath("$.reporter.displayName").value("Test Reporter"));
    }

    @Test
    void missingCoordinatesReturn400() throws Exception {
        mockMvc.perform(post("/announcements/10/sightings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"seenAt":"2026-11-25T14:30:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));

        verify(sightingService, never()).create(any(), any());
    }

    @Test
    void invalidLatitudeReturns400() throws Exception {
        mockMvc.perform(post("/announcements/10/sightings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 100,
                                  "longitude": 23.62,
                                  "seenAt": "2026-11-25T14:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("latitude"));
    }

    @Test
    void futureDateReturns400() throws Exception {
        mockMvc.perform(post("/announcements/10/sightings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 46.77,
                                  "longitude": 23.62,
                                  "seenAt": "2035-01-01T12:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("seenAt"));
    }

    @Test
    void invalidAnnouncementReturns400() throws Exception {
        when(sightingService.create(eq(10L), any()))
                .thenThrow(new InvalidRequestException(
                        "announcementId",
                        "Sightings require an active lost announcement"
                ));

        mockMvc.perform(post("/announcements/10/sightings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("announcementId"));
    }

    @Test
    void listReturnsSightings() throws Exception {
        when(sightingService.findByAnnouncementId(10L))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/announcements/10/sightings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reporter.displayName")
                        .value("Test Reporter"));
    }

    @Test
    void listIsPublic() throws Exception {
        when(sightingService.findByAnnouncementId(10L))
                .thenReturn(List.of(response()));

        mockMvc.perform(get("/announcements/10/sightings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isOk());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/announcements/10/sightings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());

        verify(sightingService, never()).create(any(), any());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/sightings/5"))
                .andExpect(status().isNoContent());

        verify(sightingService).delete(5L);
    }

    @Test
    void deleteReturns403ForOtherUsers() throws Exception {
        doThrow(new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the reporter or announcement owner can delete this sighting"
        )).when(sightingService).delete(5L);

        mockMvc.perform(delete("/sightings/5"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/sightings/5")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid"))
                .andExpect(status().isUnauthorized());

        verify(sightingService, never()).delete(any());
    }
}
