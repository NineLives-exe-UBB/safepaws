package com.ninelives.safepaws.service;

import com.ninelives.safepaws.TestcontainersConfiguration;
import com.ninelives.safepaws.dto.SightingRequest;
import com.ninelives.safepaws.dto.SightingResponse;
import com.ninelives.safepaws.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        TestcontainersConfiguration.class,
        SightingService.class,
        AnnouncementOwnershipService.class
})
class SightingServiceIntegrationTest {

    @Autowired
    private SightingService sightingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long announcementId;
    private Long ownerId;
    private Long reporterId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        announcementId = jdbcTemplate.queryForObject(
                "SELECT id FROM announcement WHERE contact_phone = '0700000001'",
                Long.class
        );

        ownerId = userId("ana@safepaws.dev");
        reporterId = userId("mihai@safepaws.dev");
        otherUserId = userId("elena@safepaws.dev");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Long userId(String email) {
        return userRepository.findByEmail(email).orElseThrow().getId();
    }

    private void logInAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId, null, List.of()
                )
        );
    }

    @Test
    void listsSightingsChronologically() {
        List<SightingResponse> sightings =
                sightingService.findByAnnouncementId(announcementId);

        assertThat(sightings).hasSize(4);

        assertThat(sightings)
                .extracting(SightingResponse::seenAt)
                .isSorted();

        assertThat(sightings)
                .extracting(s -> s.reporter().displayName())
                .contains("Mihai Rus", "Elena Moldovan");
    }

    @Test
    void createsAndSavesSightingInDatabase() {
        logInAs(reporterId);

        SightingRequest request = new SightingRequest(
                46.7712,
                23.6236,
                LocalDateTime.now().minusMinutes(10),
                "I saw the cat near the park"
        );

        SightingResponse created =
                sightingService.create(announcementId, request);

        assertThat(created.id()).isNotNull();
        assertThat(created.reporter().id()).isEqualTo(reporterId);
        assertThat(created.note()).isEqualTo("I saw the cat near the park");
        assertThat(created.createdAt()).isNotNull();

        List<SightingResponse> sightings =
                sightingService.findByAnnouncementId(announcementId);

        assertThat(sightings).hasSize(5);
        assertThat(sightings)
                .extracting(SightingResponse::id)
                .contains(created.id());
    }

    @Test
    void reporterCanDeleteOwnSighting() {
        Long sightingId = sightingService
                .findByAnnouncementId(announcementId)
                .getFirst()
                .id();

        logInAs(reporterId);

        sightingService.delete(sightingId);

        assertThat(sightingService.findByAnnouncementId(announcementId))
                .hasSize(3);
    }

    @Test
    void announcementOwnerCanDeleteSighting() {
        Long sightingId = sightingService
                .findByAnnouncementId(announcementId)
                .getFirst()
                .id();

        logInAs(ownerId);

        sightingService.delete(sightingId);

        assertThat(sightingService.findByAnnouncementId(announcementId))
                .hasSize(3);
    }

    @Test
    void otherUsersCannotDeleteSighting() {
        Long sightingId = sightingService
                .findByAnnouncementId(announcementId)
                .getFirst()
                .id();

        logInAs(otherUserId);

        assertThatThrownBy(() -> sightingService.delete(sightingId))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN)
                );

        assertThat(sightingService.findByAnnouncementId(announcementId))
                .hasSize(4);
    }
}
