package nl.johnkruidhof.musiccollection.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.importexport.ImportJobDto;
import nl.johnkruidhof.musiccollection.service.CollectionExportService;
import nl.johnkruidhof.musiccollection.service.ImportService;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class ImportExportController {

    private final CollectionExportService exportService;
    private final ImportService importService;

    /** De eigen collectie als CSV (puntkomma, UTF-8 met BOM). */
    @GetMapping(path = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt) {
        byte[] csv = exportService.exportCsv(jwt).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(exportService.fileName(Instant.now())).build().toString())
                .body(csv);
    }

    /** Collectie-export van Discogs (multipart {@code file}); start een achtergrondtaak. */
    @PostMapping(path = "/import/discogs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobDto> importDiscogs(@AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        try {
            return ResponseEntity.accepted().body(importService.startDiscogsImport(jwt, file.getBytes()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @GetMapping("/import/{jobId}")
    public ImportJobDto importStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID jobId) {
        return importService.get(jwt, jobId);
    }

    /** De laatste import van de gebruiker; 204 als er (sinds de laatste herstart) geen is. */
    @GetMapping("/import/latest")
    public ResponseEntity<ImportJobDto> latestImport(@AuthenticationPrincipal Jwt jwt) {
        ImportJobDto latest = importService.latest(jwt);
        return latest == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(latest);
    }
}
