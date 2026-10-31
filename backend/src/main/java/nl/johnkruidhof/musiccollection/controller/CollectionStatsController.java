package nl.johnkruidhof.musiccollection.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.stats.CollectionStatsDto;
import nl.johnkruidhof.musiccollection.service.CollectionStatsService;

@RestController
@RequiredArgsConstructor
public class CollectionStatsController {

    private final CollectionStatsService statsService;

    /** Aantallen per type, decennium, artiest en staat, plus de totale aankoopwaarde. */
    @GetMapping("/api/collection/stats")
    public CollectionStatsDto stats(@AuthenticationPrincipal Jwt jwt) {
        return statsService.getStats(jwt);
    }
}
