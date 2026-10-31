package nl.johnkruidhof.musiccollection.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.service.CollectionStatsService;

@WebMvcTest(CollectionStatsController.class)
@Import({SecurityConfig.class, CorsConfig.class, PropertiesConfig.class})
class CollectionStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CollectionStatsService statsService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsStats() throws Exception {
        given(statsService.getStats(any())).willReturn(new CollectionStatsDto(3, new BigDecimal("42.49"), 2,
                List.of(new CollectionStatsDto.MediaTypeCount(MediaType.VINYL, 2)),
                List.of(new CollectionStatsDto.DecadeCount(null, 1)), List.of(), List.of()));

        mockMvc.perform(get("/api/collection/stats").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalValue").value(42.49))
                .andExpect(jsonPath("$.byMediaType[0].mediaType").value("VINYL"))
                .andExpect(jsonPath("$.byDecade[0].decade").isEmpty());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/collection/stats")).andExpect(status().isUnauthorized());
    }
}
