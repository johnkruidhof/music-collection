package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import nl.johnkruidhof.musiccollection.dto.recognition.RecognitionResultDto;
import nl.johnkruidhof.musiccollection.dto.recognition.RecognizedTrackDto;
import nl.johnkruidhof.musiccollection.exception.RecognitionUnavailableException;
import nl.johnkruidhof.musiccollection.service.RecognitionService;

@WebMvcTest(RecognitionController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class RecognitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecognitionService recognitionService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void statusTellsWhetherListeningIsConfigured() throws Exception {
        given(recognitionService.enabled()).willReturn(true);

        mockMvc.perform(get("/api/recognize/status").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void recognizeReturnsTrack() throws Exception {
        given(recognitionService.recognize(any(), any(), eq("opname.webm"))).willReturn(new RecognitionResultDto(
                new RecognizedTrackDto("Radiohead", "Karma Police", "OK Computer", 1997, null, null), List.of(),
                List.of()));

        mockMvc.perform(multipart("/api/recognize").with(jwt())
                        .file(new MockMultipartFile("file", "opname.webm", "audio/webm", new byte[] {1, 2})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.track.title").value("Karma Police"));
    }

    @Test
    void notConfiguredGives503() throws Exception {
        given(recognitionService.recognize(any(), any(), any())).willThrow(new RecognitionUnavailableException());

        mockMvc.perform(multipart("/api/recognize").with(jwt())
                        .file(new MockMultipartFile("file", "opname.webm", "audio/webm", new byte[] {1})))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Luisteren niet beschikbaar"));
    }
}
