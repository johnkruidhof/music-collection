package nl.johnkruidhof.musiccollection.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleaseDto;
import nl.johnkruidhof.musiccollection.dto.admin.AdminReleasePageDto;
import nl.johnkruidhof.musiccollection.dto.admin.MergeReleasesRequest;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.service.AdminCatalogService;

/** Catalogusbeheer (F-27). Alleen rol {@code admin}: {@code /api/admin/**} in SecurityConfig. */
@RestController
@RequestMapping("/api/admin/releases")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService adminCatalogService;

    /** {@code ?q=} zoekt in artiest/titel, of exact op barcode / Discogs-id / MBID. */
    @GetMapping
    public AdminReleasePageDto search(@RequestParam(required = false) String q,
            @RequestParam(required = false) MetadataSource source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return adminCatalogService.search(q, source, page, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Alle releases zonder collectie-items en wensen verwijderen. */
    @DeleteMapping("/unused")
    public Map<String, Integer> deleteUnused() {
        return Map.of("deleted", adminCatalogService.deleteUnused());
    }

    @PostMapping("/{id}/merge")
    public AdminReleaseDto merge(@PathVariable UUID id, @Valid @RequestBody MergeReleasesRequest request) {
        return adminCatalogService.merge(id, request.targetId());
    }
}
