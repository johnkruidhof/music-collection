package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nl.johnkruidhof.musiccollection.config.CorsConfig;
import nl.johnkruidhof.musiccollection.config.PropertiesConfig;
import nl.johnkruidhof.musiccollection.config.SecurityConfig;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.dto.release.TrackDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.service.LookupService;

@WebMvcTest(LookupController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class LookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LookupService lookupService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void barcodeLookupReturnsCandidates() throws Exception {
        given(lookupService.lookupBarcode("724385522925")).willReturn(List.of(new ReleaseCandidateDto(
                MetadataSource.DISCOGS, "2149389", "Radiohead", "OK Computer", 1997, MediaType.VINYL,
                "Vinyl, LP, Album", "Parlophone", "NODATA 02", "UK", "724385522925", "https://img/thumb.jpg")));

        mockMvc.perform(get("/api/lookup/barcode/724385522925").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("DISCOGS"))
                .andExpect(jsonPath("$[0].externalId").value("2149389"))
                .andExpect(jsonPath("$[0].mediaType").value("VINYL"));
    }

    @Test
    void invalidBarcodeGivesProblemDetail() throws Exception {
        given(lookupService.lookupBarcode("abc")).willThrow(new InvalidRequestException("Ongeldige barcode"));

        mockMvc.perform(get("/api/lookup/barcode/abc").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ongeldig verzoek"));
    }

    @Test
    void searchBindsQueryParameters() throws Exception {
        given(lookupService.search(new ReleaseSearchCriteria("Radiohead", "OK Computer", 1997, MediaType.CD)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/lookup/search").with(jwt())
                        .param("artist", "Radiohead").param("title", "OK Computer")
                        .param("year", "1997").param("mediaType", "CD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        verify(lookupService).search(new ReleaseSearchCriteria("Radiohead", "OK Computer", 1997, MediaType.CD));
    }

    @Test
    void searchWithInvalidYearIsRejected() throws Exception {
        mockMvc.perform(get("/api/lookup/search").with(jwt()).param("title", "x").param("year", "1800"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(lookupService);
    }

    @Test
    void detailsIncludeTracks() throws Exception {
        given(lookupService.getDetails("discogs", "2149389")).willReturn(new ReleaseDto(null, "724385522925",
                "Radiohead", "OK Computer", 1997, MediaType.VINYL, null, null, null, null, null, null,
                MetadataSource.DISCOGS, "2149389", null, List.of(new TrackDto(1, "A1", 1, "Airbag", 284, null))));

        mockMvc.perform(get("/api/lookup/discogs/2149389").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks[0].title").value("Airbag"));
    }

    @Test
    void unknownReleaseGives404AndUnavailableSourceGives502() throws Exception {
        given(lookupService.getDetails("discogs", "1")).willThrow(new ResourceNotFoundException("Release", "discogs/1"));
        given(lookupService.getDetails("discogs", "2")).willThrow(new ExternalLookupException("Discogs", "plat", null));

        mockMvc.perform(get("/api/lookup/discogs/1").with(jwt())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/lookup/discogs/2").with(jwt()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.source").value("Discogs"));
    }

    @Test
    void lookupRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/lookup/barcode/724385522925")).andExpect(status().isUnauthorized());
    }
}
