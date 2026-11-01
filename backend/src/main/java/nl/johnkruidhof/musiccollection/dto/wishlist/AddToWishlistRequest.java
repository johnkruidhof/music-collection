package nl.johnkruidhof.musiccollection.dto.wishlist;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/** Een kandidaat op de verlanglijst zetten; zonder {@code genreId} wordt het genre voorgesteld via de artiest. */
public record AddToWishlistRequest(
        @NotNull MetadataSource source,
        @NotBlank @Size(max = 50) String externalId,
        @Size(max = 1000) String notes,
        UUID genreId) {
}
