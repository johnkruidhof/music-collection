package nl.johnkruidhof.musiccollection.controller;

import java.net.URI;
import java.util.UUID;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionPageDto;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.service.CollectionService;

@RestController
@RequestMapping("/api/collection")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    /** {@code ?q=&mediaType=&sort=artist,asc&page=0&size=24} */
    @GetMapping
    public CollectionPageDto list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) MediaType mediaType,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return collectionService.list(jwt, q, mediaType, sort, page, size);
    }

    @GetMapping("/{id}")
    public CollectionItemDto get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return collectionService.get(jwt, id);
    }

    @PostMapping
    public ResponseEntity<CollectionItemDto> add(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCollectionRequest request) {
        CollectionItemDto item = collectionService.add(jwt, request);
        return ResponseEntity.created(URI.create("/api/collection/" + item.id())).body(item);
    }

    @PutMapping("/{id}")
    public CollectionItemDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody CollectionItemFields fields) {
        return collectionService.update(jwt, id, fields);
    }

    /** Alleen voor handmatig ingevoerde releases. */
    @PutMapping("/{id}/release")
    public CollectionItemDto updateRelease(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody ManualReleaseRequest release) {
        return collectionService.updateRelease(jwt, id, release);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        collectionService.delete(jwt, id);
        return ResponseEntity.noContent().build();
    }
}
