package com.ninelives.safepaws.controller;

import com.ninelives.safepaws.dto.AnnouncementFilter;
import com.ninelives.safepaws.dto.AnnouncementRequest;
import com.ninelives.safepaws.dto.AnnouncementResponse;
import com.ninelives.safepaws.dto.AnnouncementSummaryResponse;
import com.ninelives.safepaws.dto.ErrorResponse;
import com.ninelives.safepaws.exception.AnnouncementNotFoundException;
import com.ninelives.safepaws.exception.InvalidFilterException;
import com.ninelives.safepaws.model.AnnouncementStatus;
import com.ninelives.safepaws.model.AnnouncementType;
import com.ninelives.safepaws.model.Species;
import com.ninelives.safepaws.service.AnnouncementService;
import com.ninelives.safepaws.service.PosterService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnnouncementController.class)
class AnnouncementControllerTest {

    private static final String VALID_REQUEST = """
            {
              "type": "LOST",
              "species": "DOG",
              "petName": "Oscar",
              "breed": "Golden Retriever",
              "color": "Golden",
              "distinctiveMarks": "Red nylon collar with bell",
              "eventDate": "2026-11-01T09:15:00",
              "latitude": 46.7712,
              "longitude": 23.6236,
              "area": "Mărăști, Cluj-Napoca",
              "contactName": "Ion Radu",
              "contactPhone": "0740123456"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnnouncementService announcementService;

    @MockitoBean
    private PosterService posterService;

    private static AnnouncementResponse response(Long id) {
        return response(id, AnnouncementStatus.ACTIVE);
    }

    private static AnnouncementResponse response(Long id, AnnouncementStatus status) {
        return new AnnouncementResponse(
                id, AnnouncementType.LOST, Species.DOG, "Oscar",
                "Golden Retriever", "Golden", "Red nylon collar with bell",
                LocalDateTime.of(2026, 11, 1, 9, 15),
                46.7712, 23.6236, "Mărăști, Cluj-Napoca",
                "Ion Radu", "0740123456",
                status, LocalDateTime.of(2026, 11, 1, 10, 0), List.of()
        );
    }

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post("/announcements").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Nested
    class Create {

        @Test
        void validRequestReturns201WithLocationAndBody() throws Exception {
            when(announcementService.create(any())).thenReturn(response(15L));

            postJson(VALID_REQUEST)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/announcements/15"))
                    .andExpect(jsonPath("$.id").value(15))
                    .andExpect(jsonPath("$.type").value("LOST"))
                    .andExpect(jsonPath("$.species").value("DOG"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.createdAt").value("2026-11-01T10:00:00"))
                    .andExpect(jsonPath("$.photos", hasSize(0)));
        }

        @Test
        void passesTheParsedRequestToTheService() throws Exception {
            when(announcementService.create(any())).thenReturn(response(15L));

            postJson(VALID_REQUEST).andExpect(status().isCreated());

            ArgumentCaptor<AnnouncementRequest> captor = ArgumentCaptor.forClass(AnnouncementRequest.class);
            verify(announcementService).create(captor.capture());
            AnnouncementRequest sent = captor.getValue();
            assertThat(sent.type()).isEqualTo(AnnouncementType.LOST);
            assertThat(sent.species()).isEqualTo(Species.DOG);
            assertThat(sent.eventDate()).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 15));
            assertThat(sent.contactPhone()).isEqualTo("0740123456");
        }

        @Test
        void onlyRequiredFieldsIsEnough() throws Exception {
            when(announcementService.create(any())).thenReturn(response(16L));

            postJson("""
                    {"type": "FOUND", "species": "CAT", "eventDate": "2026-11-01T09:15:00",
                     "area": "Centru", "contactName": "Ana", "contactPhone": "+40721000111"}
                    """)
                    .andExpect(status().isCreated());
        }

        @Test
        void emptyBodyListsEveryRequiredField() throws Exception {
            postJson("{}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(6)))
                    // sorted by field name
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("area"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Area is required"))
                    .andExpect(jsonPath("$.fieldErrors[1].message").value("Contact name is required"))
                    .andExpect(jsonPath("$.fieldErrors[2].message").value("Contact phone is required"))
                    .andExpect(jsonPath("$.fieldErrors[3].message").value("Event date is required"))
                    .andExpect(jsonPath("$.fieldErrors[4].message").value("Species is required"))
                    .andExpect(jsonPath("$.fieldErrors[5].message").value("Type is required"));

            verify(announcementService, never()).create(any());
        }

        @Test
        void blankTextFieldsAreRequired() throws Exception {
            postJson(VALID_REQUEST
                    .replace("\"Mărăști, Cluj-Napoca\"", "\"   \"")
                    .replace("\"Ion Radu\"", "\"\"")
                    .replace("\"0740123456\"", "\"\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(3)))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Area is required"))
                    .andExpect(jsonPath("$.fieldErrors[1].message").value("Contact name is required"))
                    // one error for an empty phone, not "required" + "invalid"
                    .andExpect(jsonPath("$.fieldErrors[2].message").value("Contact phone is required"));
        }

        @Test
        void eventDateInTheFutureIsRejected() throws Exception {
            String future = LocalDateTime.now().plusDays(1).withNano(0).toString();

            postJson(VALID_REQUEST.replace("2026-11-01T09:15:00", future))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("eventDate"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Event date cannot be in the future"));
        }

        @Test
        void invalidPhoneIsRejected() throws Exception {
            postJson(VALID_REQUEST.replace("0740123456", "07-abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("contactPhone"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Contact phone must be a valid phone number"));
        }

        @Test
        void tooLongPetNameIsRejected() throws Exception {
            postJson(VALID_REQUEST.replace("\"Oscar\"", "\"" + "a".repeat(51) + "\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("petName"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Pet name must be at most 50 characters"));
        }

        @Test
        void latitudeOutOfRangeIsRejected() throws Exception {
            postJson(VALID_REQUEST.replace("46.7712", "95"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("latitude"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Latitude must be between -90 and 90"));
        }

        @Test
        void latitudeWithoutLongitudeIsRejected() throws Exception {
            postJson(VALID_REQUEST.replace("\"longitude\": 23.6236,", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("coordinates"))
                    .andExpect(jsonPath("$.fieldErrors[0].message")
                            .value("Latitude and longitude must be provided together"));
        }

        @Test
        void unknownEnumValueListsTheAllowedValues() throws Exception {
            postJson(VALID_REQUEST.replace("\"LOST\"", "\"STOLEN\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("type"))
                    .andExpect(jsonPath("$.fieldErrors[0].message")
                            .value("Invalid value 'STOLEN', allowed values: LOST, FOUND"));
        }

        @Test
        void unreadableDateIsRejected() throws Exception {
            postJson(VALID_REQUEST.replace("2026-11-01T09:15:00", "yesterday"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("eventDate"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid value 'yesterday'"));
        }

        @Test
        void malformedJsonIsRejected() throws Exception {
            postJson("{\"type\":")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed request body"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }

        @Test
        void nonJsonContentTypeIsRejected() throws Exception {
            mockMvc.perform(post("/announcements").contentType(MediaType.TEXT_PLAIN).content("hi"))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.status").value(415))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }
    }

    @Nested
    class FindAll {
        @Test
        void returnsTheSummariesInTheOrderTheServiceGives() throws Exception {
            when(announcementService.findAll(any())).thenReturn(List.of(
                    new AnnouncementSummaryResponse(13L, AnnouncementType.LOST, "Max", Species.DOG,
                            "Someșeni, Cluj-Napoca", 46.788, 23.651, null, LocalDateTime.of(2026, 11, 2, 7, 0),
                            AnnouncementStatus.ACTIVE,
                            List.of(new AnnouncementSummaryResponse.PhotoSummary(3L, "/photos/3", true))),
                    new AnnouncementSummaryResponse(11L, AnnouncementType.FOUND, null, Species.CAT,
                            "Între Lacuri, Cluj-Napoca", null, null, null, LocalDateTime.of(2026, 11, 1, 17, 0),
                            AnnouncementStatus.RESOLVED, List.of())));

            mockMvc.perform(get("/announcements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(13))
                    .andExpect(jsonPath("$[0].type").value("LOST"))
                    .andExpect(jsonPath("$[0].petName").value("Max"))
                    .andExpect(jsonPath("$[0].species").value("DOG"))
                    .andExpect(jsonPath("$[0].area").value("Someșeni, Cluj-Napoca"))
                    .andExpect(jsonPath("$[0].latitude").value(46.788))
                    .andExpect(jsonPath("$[0].longitude").value(23.651))
                    .andExpect(jsonPath("$[0].eventDate").value("2026-11-02T07:00:00"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].photos[0].id").value(3))
                    .andExpect(jsonPath("$[0].photos[0].url").value("/photos/3"))
                    .andExpect(jsonPath("$[0].photos[0].isMain").value(true))
                    .andExpect(jsonPath("$[1].id").value(11))
                    // no map position: both keys are still sent, as null
                    .andExpect(jsonPath("$[1].latitude").value(nullValue()))
                    .andExpect(jsonPath("$[1].longitude").value(nullValue()))
                    // no location sent: no distanceKm key at all
                    .andExpect(jsonPath("$[0].distanceKm").doesNotExist())
                    // the list only carries what a card shows
                    .andExpect(jsonPath("$[0].contactPhone").doesNotExist());
        }

        @Test
        void noParametersGiveAnEmptyFilter() throws Exception {
            when(announcementService.findAll(any())).thenReturn(List.of());

            mockMvc.perform(get("/announcements")).andExpect(status().isOk());

            verify(announcementService).findAll(AnnouncementFilter.none());
        }

        @Test
        void passesEveryQueryParameterToTheService() throws Exception {
            when(announcementService.findAll(any())).thenReturn(List.of());

            mockMvc.perform(get("/announcements")
                            .param("type", "LOST")
                            .param("species", "DOG")
                            .param("status", "RESOLVED")
                            .param("color", "grey")
                            .param("breed", "terrier")
                            .param("area", "manastur")
                            .param("lat", "46.77")
                            .param("lng", "23.59")
                            .param("radiusKm", "5")
                            .param("sort", "closest"))
                    .andExpect(status().isOk());

            verify(announcementService).findAll(new AnnouncementFilter(AnnouncementType.LOST, Species.DOG,
                    AnnouncementStatus.RESOLVED, "grey", "terrier", "manastur", 46.77, 23.59, 5.0, "closest"));
        }

        @Test
        void sendsDistanceKmWhenTheServiceSetsIt() throws Exception {
            when(announcementService.findAll(any())).thenReturn(List.of(
                    new AnnouncementSummaryResponse(6L, AnnouncementType.FOUND, null, Species.DOG,
                            "Centru, Cluj-Napoca", 46.769, 23.59, 0.1, LocalDateTime.of(2026, 11, 2, 7, 0),
                            AnnouncementStatus.ACTIVE, List.of())));

            mockMvc.perform(get("/announcements").param("lat", "46.77").param("lng", "23.59"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].distanceKm").value(0.1));
        }

        @Test
        void unknownEnumValueReturns400WithTheAllowedValues() throws Exception {
            mockMvc.perform(get("/announcements").param("species", "Cat"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("species"))
                    .andExpect(jsonPath("$.fieldErrors[0].message")
                            .value("Invalid value 'Cat', allowed values: CAT, DOG, OTHER"));

            verify(announcementService, never()).findAll(any());
        }

        @Test
        void notANumberForLatReturns400() throws Exception {
            mockMvc.perform(get("/announcements").param("lat", "north").param("lng", "23.59"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("lat"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid value 'north'"));
        }

        @Test
        void invalidFilterReturns400WithTheFieldErrors() throws Exception {
            when(announcementService.findAll(any())).thenThrow(new InvalidFilterException(List.of(
                    new ErrorResponse.FieldError("sort", "sort=closest requires lat and lng"))));

            mockMvc.perform(get("/announcements").param("sort", "closest"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Some fields are invalid"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("sort"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("sort=closest requires lat and lng"));
        }

        @Test
        void noAnnouncementsGivesAnEmptyList() throws Exception {
            when(announcementService.findAll(any())).thenReturn(List.of());

            mockMvc.perform(get("/announcements"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class FindById {
        @Test
        void existingAnnouncementReturns200WithAllDetails() throws Exception {
            when(announcementService.findById(1L)).thenReturn(response(1L));

            mockMvc.perform(get("/announcements/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.petName").value("Oscar"))
                    .andExpect(jsonPath("$.contactName").value("Ion Radu"))
                    .andExpect(jsonPath("$.contactPhone").value("0740123456"))
                    .andExpect(jsonPath("$.latitude").value(46.7712))
                    .andExpect(jsonPath("$.photos", hasSize(0)));
        }

        @Test
        void missingAnnouncementReturns404() throws Exception {
            when(announcementService.findById(999L)).thenThrow(new AnnouncementNotFoundException(999L));

            mockMvc.perform(get("/announcements/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Announcement not found with id: 999"))
                    .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
        }

        @Test
        void nonNumericIdReturns400() throws Exception {
            mockMvc.perform(get("/announcements/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("id"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Invalid value 'abc'"));
        }

        @Test
        void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
            when(announcementService.findById(1L)).thenThrow(new IllegalStateException("secret internals"));

            mockMvc.perform(get("/announcements/1"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                    .andExpect(jsonPath("$.message", org.hamcrest.Matchers.not(containsString("secret"))));
        }
    }

    @Nested
    class UpdateStatus {
        @Test
        void markAnnouncementResolvedReturns200() throws Exception {
            when(announcementService.updateStatus(13L, AnnouncementStatus.RESOLVED))
                    .thenReturn(response(13L, AnnouncementStatus.RESOLVED));

            mockMvc.perform(patch("/announcements/13/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"RESOLVED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(13))
                    .andExpect(jsonPath("$.status").value("RESOLVED"));

            verify(announcementService)
                    .updateStatus(13L, AnnouncementStatus.RESOLVED);
        }

        @Test
        void reactivateAnnouncementReturns200() throws Exception {
            when(announcementService.updateStatus(13L, AnnouncementStatus.ACTIVE))
                    .thenReturn(response(13L, AnnouncementStatus.ACTIVE));

            mockMvc.perform(patch("/announcements/13/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACTIVE\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));

            verify(announcementService)
                    .updateStatus(13L, AnnouncementStatus.ACTIVE);
        }

        @Test
        void nonexistentAnnouncementReturns404() throws Exception {
            when(announcementService.updateStatus(999L, AnnouncementStatus.RESOLVED))
                    .thenThrow(new AnnouncementNotFoundException(999L));

            mockMvc.perform(patch("/announcements/999/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"RESOLVED\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        void invalidStatusReturns400() throws Exception {
            mockMvc.perform(patch("/announcements/13/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DELETED\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));

            verify(announcementService, never()).updateStatus(any(), any());
        }

        @Test
        void missingStatusReturns400() throws Exception {
            mockMvc.perform(patch("/announcements/13/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                    .andExpect(jsonPath("$.fieldErrors[0].message")
                            .value("Status is required"));

            verify(announcementService, never()).updateStatus(any(), any());
        }
    }
}