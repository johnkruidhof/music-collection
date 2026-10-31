package nl.johnkruidhof.musiccollection.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.service.LookupService;

@RestController
@RequestMapping("/api/lookup")
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    /** Kandidaten uit de lokale catalogus, anders uit Discogs/MusicBrainz; leeg = niets gevonden. */
    @GetMapping("/barcode/{barcode}")
    public List<ReleaseCandidateDto> byBarcode(@AuthenticationPrincipal Jwt jwt, @PathVariable String barcode) {
        return lookupService.lookupBarcode(jwt, barcode);
    }

    /** {@code ?artist=&title=&year=&mediaType=}; minstens artiest of titel. */
    @GetMapping("/search")
    public List<ReleaseCandidateDto> search(@AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute ReleaseSearchCriteria criteria) {
        return lookupService.search(jwt, criteria);
    }

    /** Volledige gegevens inclusief tracks; {@code source} is {@code discogs} of {@code musicbrainz}. */
    @GetMapping("/{source}/{externalId}")
    public ReleaseDto details(@PathVariable String source, @PathVariable String externalId) {
        return lookupService.getDetails(source, externalId);
    }
}
