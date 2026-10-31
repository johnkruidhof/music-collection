package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.dto.importexport.ImportJobDto;
import nl.johnkruidhof.musiccollection.service.CollectionExportService;
import nl.johnkruidhof.musiccollection.service.ImportService;

@WebMvcTest(ImportExportController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class ImportExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionExportService exportService;

    @MockitoBean
    private ImportService importService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void exportIsDownloadedAsCsvFile() throws Exception {
        given(exportService.exportCsv(any())).willReturn("Artiest;Titel\r\n");
        given(exportService.fileName(any())).willReturn("muziekcollectie-2026-10-31.csv");

        mockMvc.perform(get("/api/collection/export").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"muziekcollectie-2026-10-31.csv\""))
                .andExpect(content().string("Artiest;Titel\r\n"));
    }

    @Test
    void importStartsBackgroundJob() throws Exception {
        UUID id = UUID.randomUUID();
        given(importService.startDiscogsImport(any(), any())).willReturn(new ImportJobDto(id,
                ImportJobDto.Status.RUNNING, 3, 0, 0, 0, 0, List.of(), Instant.now(), null));

        mockMvc.perform(multipart("/api/collection/import/discogs").with(jwt())
                        .file(new MockMultipartFile("file", "export.csv", "text/csv", "x".getBytes())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void latestWithoutImportIs204() throws Exception {
        mockMvc.perform(get("/api/collection/import/latest").with(jwt())).andExpect(status().isNoContent());
    }
}
