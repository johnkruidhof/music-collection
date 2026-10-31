package nl.johnkruidhof.musiccollection.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.wishlist.AddToWishlistRequest;
import nl.johnkruidhof.musiccollection.dto.wishlist.WishlistItemDto;
import nl.johnkruidhof.musiccollection.service.WishlistService;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<WishlistItemDto> list(@AuthenticationPrincipal Jwt jwt) {
        return wishlistService.list(jwt);
    }

    /** Idempotent: staat de release er al op, dan komt de bestaande wens terug. */
    @PostMapping
    public WishlistItemDto add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddToWishlistRequest request) {
        return wishlistService.add(jwt, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        wishlistService.delete(jwt, id);
        return ResponseEntity.noContent().build();
    }

    /** Gekocht: verplaatst naar de collectie; body met eigen velden is optioneel. */
    @PostMapping("/{id}/acquire")
    public ResponseEntity<CollectionItemDto> acquire(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody(required = false) CollectionItemFields fields) {
        CollectionItemDto item = wishlistService.acquire(jwt, id, fields);
        return ResponseEntity.created(URI.create("/api/collection/" + item.id())).body(item);
    }
}
