package nl.johnkruidhof.musiccollection.dto.collection;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;

/**
 * Toevoegen aan de collectie: óf een gekozen kandidaat ({@code source} + {@code externalId}), óf een
 * handmatig ingevoerde {@code release}. {@code fields} (persoonlijke gegevens) is optioneel.
 */
public record AddToCollectionRequest(
        MetadataSource source,
        @Size(max = 50) String externalId,
        @Valid ManualReleaseRequest release,
        @Valid CollectionItemFields fields) {
}
