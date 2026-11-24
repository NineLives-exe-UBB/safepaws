package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.NotificationAreaResponse;
import com.ninelives.safepaws.dto.NotificationResponse;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.service.NotificationService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP layer for notification endpoints, with the real security config (TestSecurityConfig logs in as user 1).
 */
@WebMvcTest(NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 10, 5, 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    // ── Without a valid token every endpoint is 401 ───────────────────────────

    @Test
    void withoutAValidTokenEveryEndpointIs401() throws Exception {
        String invalid = "Bearer not-a-token";

        mockMvc.perform(get("/notifications/area").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"))
                .andExpect(jsonPath("$.fieldErrors").isArray())   // ticket item 7: [] not null
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));

        mockMvc.perform(put("/notifications/area").header(HttpHeaders.AUTHORIZATION, invalid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":46.77,\"longitude\":23.62,\"radiusKm\":5}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/notifications/area").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/notifications/unread-count").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/notifications/1/read").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/notifications/read-all").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never()).getArea();
        verify(notificationService, never()).saveArea(any());
    }

    // ── GET /notifications/area ───────────────────────────────────────────────

    @Test
    void getAreaReturnsTheArea() throws Exception {
        when(notificationService.getArea()).thenReturn(new NotificationAreaResponse(46.7571, 23.5547, 3));

        mockMvc.perform(get("/notifications/area"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(46.7571))
                .andExpect(jsonPath("$.longitude").value(23.5547))
                .andExpect(jsonPath("$.radiusKm").value(3));
    }

    @Test
    void getAreaReturns404WhenNoneSet() throws Exception {
        when(notificationService.getArea())
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification area is not set"));

        mockMvc.perform(get("/notifications/area"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification area is not set"));
    }

    // ── PUT /notifications/area ───────────────────────────────────────────────

    @Test
    void putAreaSavesAndReturnsIt() throws Exception {
        when(notificationService.saveArea(any()))
                .thenReturn(new NotificationAreaResponse(46.7571, 23.5547, 5));

        mockMvc.perform(put("/notifications/area").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":46.7571,\"longitude\":23.5547,\"radiusKm\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.radiusKm").value(5));
    }

    @Test
    void putAreaWithMissingLatitudeReturns400() throws Exception {
        mockMvc.perform(put("/notifications/area").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longitude\":23.55,\"radiusKm\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("latitude"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Latitude is required"));

        verify(notificationService, never()).saveArea(any());
    }

    @Test
    void putAreaWithRadiusOutOfRangeReturns400() throws Exception {
        mockMvc.perform(put("/notifications/area").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":46.77,\"longitude\":23.62,\"radiusKm\":21}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("radiusKm"));
    }

    @Test
    void putAreaWithRadius0Returns400() throws Exception {
        mockMvc.perform(put("/notifications/area").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"latitude\":46.77,\"longitude\":23.62,\"radiusKm\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("radiusKm"));
    }

    // ── DELETE /notifications/area ────────────────────────────────────────────

    @Test
    void deleteAreaReturns204() throws Exception {
        mockMvc.perform(delete("/notifications/area"))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteArea();
    }

    // ── GET /notifications ────────────────────────────────────────────────────

    @Test
    void listNotificationsReturnsContractShape() throws Exception {
        NotificationResponse notification = new NotificationResponse(
                7L,
                new NotificationResponse.AnnouncementSummary(31L, AnnouncementType.LOST, Species.DOG, "Bruno",
                        "Grigorescu, Cluj-Napoca"),
                NOW,
                false
        );
        when(notificationService.listNotifications()).thenReturn(List.of(notification));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].announcement.id").value(31))
                .andExpect(jsonPath("$[0].announcement.type").value("LOST"))
                .andExpect(jsonPath("$[0].announcement.species").value("DOG"))
                .andExpect(jsonPath("$[0].announcement.petName").value("Bruno"))
                .andExpect(jsonPath("$[0].announcement.area").value("Grigorescu, Cluj-Napoca"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-11-26T10:05:00"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void listNotificationsReturnsEmptyList() throws Exception {
        when(notificationService.listNotifications()).thenReturn(List.of());

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── GET /notifications/unread-count ──────────────────────────────────────

    @Test
    void unreadCountReturnsCount() throws Exception {
        when(notificationService.unreadCount()).thenReturn(2L);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));
    }

    // ── PATCH /notifications/{id}/read ────────────────────────────────────────

    @Test
    void markAsReadReturns204() throws Exception {
        mockMvc.perform(patch("/notifications/7/read"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(7L);
    }

    @Test
    void markAsReadReturns404ForUnknownOrOtherUsersNotification() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: 999"))
                .when(notificationService).markAsRead(999L);

        mockMvc.perform(patch("/notifications/999/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found with id: 999"));
    }

    // ── PATCH /notifications/read-all ─────────────────────────────────────────

    @Test
    void markAllAsReadReturns204() throws Exception {
        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead();
    }
}
