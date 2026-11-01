package nl.johnkruidhof.musiccollection.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.genre.GenreDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreImportResultDto;
import nl.johnkruidhof.musiccollection.dto.genre.GenreRequest;
import nl.johnkruidhof.musiccollection.service.GenreService;

/** Eigen genres (F-28). */
@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    /** Hoofdgenres met subgenres, alfabetisch; de eerste keer met de standaardgenres. */
    @GetMapping
    public List<GenreDto> list(@AuthenticationPrincipal Jwt jwt) {
        return genreService.list(jwt);
    }

    @PostMapping
    public ResponseEntity<GenreDto> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody GenreRequest request) {
        return ResponseEntity.status(201).body(genreService.create(jwt, request));
    }

    @PutMapping("/{id}")
    public GenreDto rename(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody GenreRequest request) {
        return genreService.rename(jwt, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        genreService.delete(jwt, id);
        return ResponseEntity.noContent().build();
    }

    /** Boomstructuur van een muziekmap (tekstbestand, multipart {@code file}). */
    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenreImportResultDto importTree(@AuthenticationPrincipal Jwt jwt, @RequestPart("file") MultipartFile file) {
        try {
            return genreService.importTree(jwt, file.getBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
