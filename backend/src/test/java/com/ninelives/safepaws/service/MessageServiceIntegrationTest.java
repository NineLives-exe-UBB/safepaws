package com.ninelives.safepaws.service;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.dto.ConversationResponse;
import com.ninelives.safepaws.dto.MessageRequest;
import com.ninelives.safepaws.dto.MessageResponse;
import com.ninelives.safepaws.exception.InvalidRequestException;
import com.ninelives.safepaws.model.User;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Messages against the real database, starting from the V9 demo conversations about Mitzi:
 * Ana owns her; Mihai and Elena wrote to her. Each test is rolled back afterwards.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, MessageService.class, AnnouncementOwnershipService.class})
class MessageServiceIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long mitzi;
    private Long ana;
    private Long mihai;
    private Long elena;

    @BeforeEach
    void findTheDemoData() {
        mitzi = jdbcTemplate.queryForObject("SELECT id FROM announcement WHERE contact_phone = '0700000001'", Long.class);
        ana = idOf("ana@safepaws.dev");
        mihai = idOf("mihai@safepaws.dev");
        elena = idOf("elena@safepaws.dev");
    }

    @AfterEach
    void logOut() {
        SecurityContextHolder.clearContext();
    }

    private Long idOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    /** Logs in the way JwtAuthenticationFilter does: the principal is the user id. */
    private void logInAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    @Test
    void theOwnerSeesOneConversationPerPersonNewestFirst() {
        logInAs(ana);

        List<ConversationResponse> conversations = messageService.conversations();

        assertThat(conversations).hasSize(2);
        ConversationResponse withMihai = conversations.get(0);
        assertThat(withMihai.announcementId()).isEqualTo(mitzi);
        assertThat(withMihai.petName()).isEqualTo("Mitzi");
        assertThat(withMihai.otherUser()).isEqualTo(new ConversationResponse.OtherUser(mihai, "Mihai Rus"));
        assertThat(withMihai.lastMessage().body()).startsWith("I saw her again this evening");
        assertThat(withMihai.lastMessage().fromMe()).isFalse();
        assertThat(withMihai.unreadCount()).isEqualTo(1);

        ConversationResponse withElena = conversations.get(1);
        assertThat(withElena.otherUser().displayName()).isEqualTo("Elena Moldovan");
        assertThat(withElena.lastMessage().fromMe()).isTrue();
        assertThat(withElena.unreadCount()).isZero();
    }

    @Test
    void theWriterSeesTheirConversationWithTheOwner() {
        logInAs(elena);

        assertThat(messageService.conversations())
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.otherUser().displayName()).isEqualTo("Ana Pop");
                    assertThat(c.unreadCount()).isEqualTo(1);
                });
        assertThat(messageService.unreadCount()).isEqualTo(1);
    }

    @Test
    void openingAConversationReturnsItOldestFirstAndMarksItRead() {
        logInAs(ana);
        assertThat(messageService.unreadCount()).isEqualTo(1);

        List<MessageResponse> messages = messageService.readConversation(mitzi, mihai);

        assertThat(messages).hasSize(4);
        assertThat(messages).extracting(MessageResponse::createdAt).isSorted();
        assertThat(messages.getFirst().body()).startsWith("Hi! I think I saw Mitzi");
        assertThat(messages).filteredOn(m -> m.recipientId().equals(ana))
                .allSatisfy(m -> assertThat(m.readAt()).isNotNull());
        assertThat(messageService.unreadCount()).isZero();
    }

    @Test
    void openingOnlyMarksWhatIReceivedNotWhatTheOtherPersonHasNotRead() {
        logInAs(ana);

        messageService.readConversation(mitzi, elena);

        // Ana's reply to Elena stays unread until Elena opens it
        logInAs(elena);
        assertThat(messageService.unreadCount()).isEqualTo(1);
    }

    @Test
    void aWriterCanLeaveOutWithToOpenTheirConversationWithTheOwner() {
        logInAs(mihai);

        assertThat(messageService.readConversation(mitzi, null)).hasSize(4);
    }

    @Test
    void someoneWhoWritesAndTheOwnerWhoAnswersBuildOneConversation() {
        User newUser = userRepository.saveAndFlush(new User("new@safepaws.dev",
                "$2a$10$G1ImbrhJGVGqqR1klzQNFeytHmxZM.94vxa1rLR5LsEY0K2at.rvO", "New User"));

        logInAs(newUser.getId());
        MessageResponse question = messageService.send(mitzi, new MessageRequest("  Is she still missing?  ", null));
        assertThat(question.recipientId()).isEqualTo(ana);
        assertThat(question.body()).isEqualTo("Is she still missing?");
        assertThat(question.createdAt()).isNotNull();
        assertThat(question.createdAt().getNano()).isZero();
        assertThat(question.readAt()).isNull();

        logInAs(ana);
        MessageResponse answer = messageService.send(mitzi, new MessageRequest("Yes, thank you!", newUser.getId()));
        assertThat(answer.recipientId()).isEqualTo(newUser.getId());
        assertThat(messageService.conversations()).hasSize(3);
        assertThat(messageService.conversations().getFirst().otherUser().id()).isEqualTo(newUser.getId());

        logInAs(newUser.getId());
        assertThat(messageService.readConversation(mitzi, null))
                .extracting(MessageResponse::body)
                .containsExactly("Is she still missing?", "Yes, thank you!");
    }

    @Test
    void theOwnerCannotWriteToSomeoneWhoNeverWrote() {
        User stranger = userRepository.saveAndFlush(new User("stranger@safepaws.dev",
                "$2a$10$G1ImbrhJGVGqqR1klzQNFeytHmxZM.94vxa1rLR5LsEY0K2at.rvO", "Stranger"));
        logInAs(ana);

        assertThatThrownBy(() -> messageService.send(mitzi, new MessageRequest("Hello", stranger.getId())))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> messageService.send(mitzi, new MessageRequest("Hello", ana)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void someoneElseCannotReadAConversationTheyAreNotIn() {
        logInAs(elena);

        assertThatThrownBy(() -> messageService.readConversation(mitzi, mihai))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You can only read your own conversations");
    }

    @Test
    void theDatabaseRejectsAMessageToYourself() {
        // the last statement of this test: after an error Postgres aborts the rest of the transaction
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO message (announcement_id, sender_id, recipient_id, body) VALUES (?, ?, ?, 'hi')",
                mitzi, ana, ana))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
