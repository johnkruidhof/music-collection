package nl.johnkruidhof.musiccollection.dto.wishlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/** Een kandidaat op de verlanglijst zetten. */
public record AddToWishlistRequest(
        @NotNull MetadataSource source,
        @NotBlank @Size(max = 50) String externalId,
        @Size(max = 1000) String notes) {
}
