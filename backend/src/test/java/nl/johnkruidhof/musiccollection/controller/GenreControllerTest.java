package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.dto.genre.GenreDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreImportResultDto;
import nl.johnkruidhof.musiccollection.service.GenreService;

@WebMvcTest(GenreController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenreService genreService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listsGenresWithSubgenres() throws Exception {
        UUID metal = UUID.randomUUID();
        given(genreService.list(any())).willReturn(List.of(new GenreDto(metal, "Hardrock - Metal", null, 3,
                List.of(new GenreDto(UUID.randomUUID(), "Doom Metal", metal, 1, List.of())))));

        mockMvc.perform(get("/api/genres").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hardrock - Metal"))
                .andExpect(jsonPath("$[0].subgenres[0].name").value("Doom Metal"));
    }

    @Test
    void createValidatesName() throws Exception {
        mockMvc.perform(post("/api/genres").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(genreService);
    }

    @Test
    void importAcceptsTreeFile() throws Exception {
        given(genreService.importTree(any(), any())).willReturn(new GenreImportResultDto(1, 2, 3, 4, 5));

        mockMvc.perform(multipart("/api/genres/import").with(jwt())
                        .file(new MockMultipartFile("file", "boomstructuur.txt", "text/plain", "|-- Rock".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemsAssigned").value(4));
    }
}
