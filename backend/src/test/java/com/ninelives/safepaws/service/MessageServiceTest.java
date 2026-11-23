package com.ninelives.safepaws.service;

import com.ninelives.safepaws.dto.MessageRequest;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.model.Announcement;
import com.ninelives.safepaws.model.Message;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.AnnouncementRepository;
import com.ninelives.safepaws.repository.MessageRepository;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules of who may write and read, without a database. Ana (1) owns announcement 10.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final long ANA = 1L;
    private static final long MIHAI = 2L;
    private static final long ELENA = 3L;
    private static final long ANNOUNCEMENT = 10L;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AnnouncementOwnershipService ownershipService;

    @InjectMocks
    private MessageService messageService;

    private static User user(long id) {
        User user = new User("u" + id + "@safepaws.dev", "hash", "User " + id);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void announcementOwnedByAna() {
        Announcement announcement = new Announcement();
        ReflectionTestUtils.setField(announcement, "id", ANNOUNCEMENT);
        announcement.setOwner(user(ANA));
        when(announcementRepository.findById(ANNOUNCEMENT)).thenReturn(Optional.of(announcement));
    }

    private void loggedInAs(long userId) {
        when(ownershipService.currentUser()).thenReturn(user(userId));
    }

    private void savesWhatItGets() {
        when(userRepository.getReferenceById(anyLong())).thenAnswer(call -> user(call.getArgument(0)));
        when(messageRepository.saveAndFlush(any())).thenAnswer(call -> {
            Message message = call.getArgument(0);
            ReflectionTestUtils.setField(message, "id", 99L);
            return message;
        });
    }

    @Test
    void aNonOwnerWritesToTheOwner() {
        loggedInAs(MIHAI);
        announcementOwnedByAna();
        savesWhatItGets();

        var response = messageService.send(ANNOUNCEMENT, new MessageRequest("I saw her", null));

        assertThat(response.senderId()).isEqualTo(MIHAI);
        assertThat(response.recipientId()).isEqualTo(ANA);
        assertThat(response.body()).isEqualTo("I saw her");
    }

    @Test
    void aNonOwnerMayNameTheOwnerAsRecipient() {
        loggedInAs(MIHAI);
        announcementOwnedByAna();
        savesWhatItGets();

        assertThat(messageService.send(ANNOUNCEMENT, new MessageRequest("Hi", ANA)).recipientId()).isEqualTo(ANA);
    }

    @Test
    void aNonOwnerCannotWriteToSomeoneElse() {
        loggedInAs(MIHAI);
        announcementOwnedByAna();

        assertThatThrownBy(() -> messageService.send(ANNOUNCEMENT, new MessageRequest("Hi", ELENA)))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("You can only write in your own conversations");
                });
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void theOwnerMustSayWhoTheyAnswer() {
        loggedInAs(ANA);
        announcementOwnedByAna();

        assertThatThrownBy(() -> messageService.send(ANNOUNCEMENT, new MessageRequest("Thanks", null)))
                .isInstanceOfSatisfying(InvalidRequestException.class, ex -> assertThat(ex.getFieldErrors())
                        .singleElement()
                        .satisfies(error -> {
                            assertThat(error.field()).isEqualTo("recipientId");
                            assertThat(error.message()).isEqualTo("Recipient is required");
                        }));
    }

    @Test
    void theOwnerCanOnlyAnswerSomeoneWhoWrote() {
        loggedInAs(ANA);
        announcementOwnedByAna();
        when(messageRepository.existsByAnnouncement_IdAndSender_IdAndRecipient_Id(ANNOUNCEMENT, ELENA, ANA))
                .thenReturn(false);

        assertThatThrownBy(() -> messageService.send(ANNOUNCEMENT, new MessageRequest("Thanks", ELENA)))
                .isInstanceOfSatisfying(InvalidRequestException.class, ex -> assertThat(ex.getFieldErrors())
                        .singleElement()
                        .satisfies(error -> assertThat(error.message())
                                .isEqualTo("Recipient has not written about this announcement")));
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void theOwnerAnswersSomeoneWhoWrote() {
        loggedInAs(ANA);
        announcementOwnedByAna();
        savesWhatItGets();
        when(messageRepository.existsByAnnouncement_IdAndSender_IdAndRecipient_Id(ANNOUNCEMENT, MIHAI, ANA))
                .thenReturn(true);

        var response = messageService.send(ANNOUNCEMENT, new MessageRequest("Thanks", MIHAI));

        assertThat(response.senderId()).isEqualTo(ANA);
        assertThat(response.recipientId()).isEqualTo(MIHAI);
    }

    @Test
    void writingAboutAMissingAnnouncementIs404() {
        loggedInAs(MIHAI);
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.send(999L, new MessageRequest("Hi", null)))
                .isInstanceOf(AnnouncementNotFoundException.class)
                .hasMessage("Announcement not found with id: 999");
    }

    @Test
    void aNonOwnerWithoutWithReadsTheirConversationWithTheOwner() {
        when(ownershipService.currentUserId()).thenReturn(MIHAI);
        announcementOwnedByAna();

        messageService.readConversation(ANNOUNCEMENT, null);

        verify(messageRepository).markAsRead(eq(ANNOUNCEMENT), eq(MIHAI), eq(ANA), any());
        verify(messageRepository).findConversation(ANNOUNCEMENT, MIHAI, ANA);
    }

    @Test
    void theOwnerMustSayWhichConversationToOpen() {
        when(ownershipService.currentUserId()).thenReturn(ANA);
        announcementOwnedByAna();

        assertThatThrownBy(() -> messageService.readConversation(ANNOUNCEMENT, null))
                .isInstanceOfSatisfying(InvalidRequestException.class, ex -> assertThat(ex.getFieldErrors())
                        .singleElement()
                        .satisfies(error -> {
                            assertThat(error.field()).isEqualTo("with");
                            assertThat(error.message()).isEqualTo("Conversation partner is required");
                        }));
    }

    @Test
    void aConversationWithoutTheOwnerCannotBeRead() {
        when(ownershipService.currentUserId()).thenReturn(ELENA);
        announcementOwnedByAna();

        assertThatThrownBy(() -> messageService.readConversation(ANNOUNCEMENT, MIHAI))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getReason()).isEqualTo("You can only read your own conversations");
                });
        verify(messageRepository, never()).markAsRead(any(), any(), any(), any());
    }

    @Test
    void readingAMissingAnnouncementIs404BeforeAny403() {
        when(ownershipService.currentUserId()).thenReturn(ELENA);
        when(announcementRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.readConversation(999L, MIHAI))
                .isInstanceOf(AnnouncementNotFoundException.class);
    }

    @Test
    void unreadCountIsWhatIReceivedAndHaveNotOpened() {
        when(ownershipService.currentUserId()).thenReturn(ANA);
        when(messageRepository.countByRecipient_IdAndReadAtIsNull(ANA)).thenReturn(3L);

        assertThat(messageService.unreadCount()).isEqualTo(3);
    }
}
