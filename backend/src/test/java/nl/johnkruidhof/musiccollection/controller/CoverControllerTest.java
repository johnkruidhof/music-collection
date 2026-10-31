package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.exception.UnsupportedImageException;
import nl.johnkruidhof.musiccollection.service.CoverService;

@WebMvcTest(CoverController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class CoverControllerTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoverService coverService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void coverIsPublicAndCacheable() throws Exception {
        given(coverService.get(ID, true)).willReturn(new CoverService.CoverData(new byte[] {1, 2, 3}, "image/jpeg"));

        mockMvc.perform(get("/api/covers/" + ID + "/thumbnail"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}))
                .andExpect(header().string("Cache-Control", "max-age=31536000, public, immutable"));
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/collection/" + ID + "/cover")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[] {1})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unreadableImageGives400() throws Exception {
        given(coverService.upload(any(), eq(ID), any())).willThrow(new UnsupportedImageException("Onbekend formaat"));

        mockMvc.perform(multipart("/api/collection/" + ID + "/cover").with(jwt())
                        .file(new MockMultipartFile("file", "cover.txt", "text/plain", "x".getBytes())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingFileGives400() throws Exception {
        mockMvc.perform(multipart("/api/collection/" + ID + "/cover").with(jwt()))
                .andExpect(status().isBadRequest());
    }
}
