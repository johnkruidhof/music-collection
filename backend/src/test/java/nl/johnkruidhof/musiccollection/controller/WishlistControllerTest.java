package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.service.WishlistService;

@WebMvcTest(WishlistController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class WishlistControllerTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WishlistService wishlistService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void addValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/wishlist").with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"DISCOGS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acquireWithoutBodyReturns201() throws Exception {
        UUID itemId = UUID.randomUUID();
        given(wishlistService.acquire(any(), eq(ID), isNull())).willReturn(new CollectionItemDto(itemId,
                new ReleaseDto(UUID.randomUUID(), null, "Radiohead", "Kid A", 2000,
                        nl.johnkruidhof.musiccollection.entity.MediaType.VINYL, null, null, null, null, null, null,
                        MetadataSource.DISCOGS, "1", null, List.of()),
                null, null, null, null, null, null, null, Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/wishlist/" + ID + "/acquire").with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/collection/" + itemId));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/wishlist/" + ID).with(jwt())).andExpect(status().isNoContent());
        verify(wishlistService).delete(any(), eq(ID));
    }
}
