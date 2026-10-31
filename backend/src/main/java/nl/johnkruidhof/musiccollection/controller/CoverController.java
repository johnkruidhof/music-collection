package nl.johnkruidhof.musiccollection.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.service.CoverService;

@RestController
@RequiredArgsConstructor
public class CoverController {

    private final CoverService coverService;

    /** Multipart-veld {@code file}; alleen voor handmatige releases. */
    @PostMapping(path = "/api/collection/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CollectionItemDto upload(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        try {
            return coverService.upload(jwt, id, file.getBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @DeleteMapping("/api/collection/{id}/cover")
    public CollectionItemDto delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return coverService.delete(jwt, id);
    }

    /** Publiek (zie SecurityConfig); een nieuwe upload krijgt een nieuw id, dus mag lang gecachet worden. */
    @GetMapping("/api/covers/{coverId}")
    public ResponseEntity<byte[]> image(@PathVariable UUID coverId) {
        return image(coverService.get(coverId, false));
    }

    @GetMapping("/api/covers/{coverId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable UUID coverId) {
        return image(coverService.get(coverId, true));
    }

    private static ResponseEntity<byte[]> image(CoverService.CoverData cover) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(cover.data());
    }
}
