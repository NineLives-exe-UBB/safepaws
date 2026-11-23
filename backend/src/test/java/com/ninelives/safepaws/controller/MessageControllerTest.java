package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.ConversationResponse;
import com.ninelives.safepaws.dto.MessageRequest;
import com.ninelives.safepaws.dto.MessageResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.service.MessageService;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP layer of the messages, with the real security config (TestSecurityConfig logs in as user 1).
 */
@WebMvcTest(MessageController.class)
@Import(TestSecurityConfig.class)
class MessageControllerTest {

    private static final LocalDateTime SENT = LocalDateTime.of(2026, 11, 25, 18, 40);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    private static MessageResponse message(Long id) {
        return new MessageResponse(id, 2L, 1L, "Hi! I think I saw your cat near the park.", SENT, null);
    }

    @Test
    void sendReturns201WithTheMessage() throws Exception {
        when(messageService.send(eq(1L), any())).thenReturn(message(12L));

        mockMvc.perform(post("/announcements/1/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Hi! I think I saw your cat near the park.\", \"recipientId\": null}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.senderId").value(2))
                .andExpect(jsonPath("$.recipientId").value(1))
                .andExpect(jsonPath("$.body").value("Hi! I think I saw your cat near the park."))
                .andExpect(jsonPath("$.createdAt").value("2026-11-25T18:40:00"))
                .andExpect(jsonPath("$.readAt").value((Object) null));

        verify(messageService).send(1L, new MessageRequest("Hi! I think I saw your cat near the park.", null));
    }

    @Test
    void emptyMessageReturns400() throws Exception {
        mockMvc.perform(post("/announcements/1/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("body"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Message is required"));

        verify(messageService, never()).send(any(), any());
    }

    @Test
    void messageOf1001CharactersReturns400And1000IsFine() throws Exception {
        mockMvc.perform(post("/announcements/1/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"" + "a".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Message must be at most 1000 characters"));

        when(messageService.send(eq(1L), any())).thenReturn(message(12L));
        mockMvc.perform(post("/announcements/1/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"" + "a".repeat(1000) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void withoutAValidTokenEveryEndpointIs401() throws Exception {
        String invalid = "Bearer not-a-token";

        mockMvc.perform(post("/announcements/1/messages").header(HttpHeaders.AUTHORIZATION, invalid)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\": \"\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication is required"));
        mockMvc.perform(get("/announcements/1/messages").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/conversations").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/messages/unread-count").header(HttpHeaders.AUTHORIZATION, invalid))
                .andExpect(status().isUnauthorized());

        verify(messageService, never()).send(any(), any());
    }

    @Test
    void ownerWithoutRecipientReturns400WithTheField() throws Exception {
        when(messageService.send(eq(1L), any()))
                .thenThrow(new InvalidRequestException("recipientId", "Recipient is required"));

        mockMvc.perform(post("/announcements/1/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Thanks\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("recipientId"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("Recipient is required"));
    }

    @Test
    void unknownAnnouncementReturns404() throws Exception {
        when(messageService.send(eq(999L), any())).thenThrow(new AnnouncementNotFoundException(999L));

        mockMvc.perform(post("/announcements/999/messages").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Hi\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Announcement not found with id: 999"));
    }

    @Test
    void readingSomeoneElsesConversationReturns403() throws Exception {
        when(messageService.readConversation(1L, 2L)).thenThrow(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only read your own conversations"));

        mockMvc.perform(get("/announcements/1/messages").param("with", "2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("You can only read your own conversations"));
    }

    @Test
    void readReturnsTheMessagesAndPassesWith() throws Exception {
        when(messageService.readConversation(1L, 2L)).thenReturn(List.of(message(12L), message(13L)));

        mockMvc.perform(get("/announcements/1/messages").param("with", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(12))
                .andExpect(jsonPath("$[1].id").value(13));
    }

    @Test
    void readWithoutWithPassesNull() throws Exception {
        when(messageService.readConversation(eq(1L), isNull())).thenReturn(List.of());

        mockMvc.perform(get("/announcements/1/messages")).andExpect(status().isOk());

        verify(messageService).readConversation(1L, null);
    }

    @Test
    void withThatIsNotANumberReturns400() throws Exception {
        mockMvc.perform(get("/announcements/1/messages").param("with", "mihai"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("with"));
    }

    @Test
    void conversationsHaveTheContractShape() throws Exception {
        when(messageService.conversations()).thenReturn(List.of(new ConversationResponse(
                1L, "Mitzi", Species.CAT, AnnouncementType.LOST,
                new ConversationResponse.OtherUser(2L, "Mihai Rus"),
                new ConversationResponse.LastMessage("Hi! I think I saw your cat...", SENT, false),
                1)));

        mockMvc.perform(get("/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].announcementId").value(1))
                .andExpect(jsonPath("$[0].petName").value("Mitzi"))
                .andExpect(jsonPath("$[0].species").value("CAT"))
                .andExpect(jsonPath("$[0].type").value("LOST"))
                .andExpect(jsonPath("$[0].otherUser.id").value(2))
                .andExpect(jsonPath("$[0].otherUser.displayName").value("Mihai Rus"))
                .andExpect(jsonPath("$[0].lastMessage.body").value("Hi! I think I saw your cat..."))
                .andExpect(jsonPath("$[0].lastMessage.createdAt").value("2026-11-25T18:40:00"))
                .andExpect(jsonPath("$[0].lastMessage.fromMe").value(false))
                .andExpect(jsonPath("$[0].unreadCount").value(1));
    }

    @Test
    void unreadCountReturnsCount() throws Exception {
        when(messageService.unreadCount()).thenReturn(3L);

        mockMvc.perform(get("/messages/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }
}
