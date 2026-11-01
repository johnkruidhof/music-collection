package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionPageDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.service.CollectionService;

@WebMvcTest(CollectionController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class CollectionControllerTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionService collectionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listPassesFiltersAndReturnsPage() throws Exception {
        given(collectionService.list(any(), eq("radio"), eq(nl.johnkruidhof.musiccollection.entity.MediaType.VINYL), org.mockito.ArgumentMatchers.isNull(),
                eq("artist,desc"), eq(1), eq(12))).willReturn(new CollectionPageDto(List.of(new CollectionItemSummaryDto(
                ID, UUID.randomUUID(), "Radiohead", "OK Computer", 1997,
                nl.johnkruidhof.musiccollection.entity.MediaType.VINYL, "2×Vinyl", null, null, "Kast A", null, null, null,
                Instant.parse("2026-10-31T12:00:00Z"))), 1, 12, 13, 2));

        mockMvc.perform(get("/api/collection").with(jwt()).param("q", "radio").param("mediaType", "VINYL")
                        .param("sort", "artist,desc").param("page", "1").param("size", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("OK Computer"))
                .andExpect(jsonPath("$.totalElements").value(13));
    }

    @Test
    void addReturns201WithLocation() throws Exception {
        given(collectionService.add(any(), eq(new AddToCollectionRequest(MetadataSource.DISCOGS, "2149389", null, null))))
                .willReturn(item());

        mockMvc.perform(post("/api/collection").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"DISCOGS\",\"externalId\":\"2149389\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/collection/" + ID))
                .andExpect(jsonPath("$.release.title").value("OK Computer"));
    }

    @Test
    void invalidManualReleaseIsRejectedWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/collection").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"release\":{\"artist\":\"\",\"title\":\"X\",\"releaseYear\":3000}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['release.artist']").exists())
                .andExpect(jsonPath("$.errors['release.mediaType']").exists())
                .andExpect(jsonPath("$.errors['release.releaseYear']").exists());
        verifyNoInteractions(collectionService);
    }

    @Test
    void updateValidatesFields() throws Exception {
        mockMvc.perform(put("/api/collection/" + ID).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"purchasePrice\":-1}"))
                .andExpect(status().isBadRequest());

        given(collectionService.update(any(), eq(ID), any())).willReturn(item());
        mockMvc.perform(put("/api/collection/" + ID).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conditionMedia\":\"NEAR_MINT\",\"storageLocation\":\"Kast A\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void someoneElsesItemIs404() throws Exception {
        given(collectionService.get(any(), eq(ID))).willThrow(new ResourceNotFoundException("Collectie-item", ID));

        mockMvc.perform(get("/api/collection/" + ID).with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/collection/" + ID).with(jwt())).andExpect(status().isNoContent());
        verify(collectionService).delete(any(), eq(ID));
    }

    @Test
    void collectionRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/collection")).andExpect(status().isUnauthorized());
    }

    private static CollectionItemDto item() {
        ReleaseDto release = new ReleaseDto(UUID.randomUUID(), null, "Radiohead", "OK Computer", 1997,
                nl.johnkruidhof.musiccollection.entity.MediaType.VINYL, null, null, null, null, null, null,
                MetadataSource.DISCOGS, "2149389", null, List.of());
        return new CollectionItemDto(ID, release, MediaCondition.NEAR_MINT, null, null, null, null, "Kast A", null, null, null, null,
                Instant.now(), Instant.now());
    }
}
