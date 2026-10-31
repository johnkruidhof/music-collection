package nl.johnkruidhof.musiccollection.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;

@Mapper(uses = ReleaseMapper.class)
public interface CollectionItemMapper {

    CollectionItemDto toDto(CollectionItem item);

    @Mapping(target = "releaseId", source = "release.id")
    @Mapping(target = "artist", source = "release.artist")
    @Mapping(target = "title", source = "release.title")
    @Mapping(target = "releaseYear", source = "release.releaseYear")
    @Mapping(target = "mediaType", source = "release.mediaType")
    @Mapping(target = "formatDetails", source = "release.formatDetails")
    @Mapping(target = "coverThumbnailUrl", source = "release.coverThumbnailUrl")
    CollectionItemSummaryDto toSummary(CollectionItem item);

    /** Overschrijft alle persoonlijke velden (ook met {@code null}). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "release", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void applyFields(CollectionItemFields fields, @MappingTarget CollectionItem item);
}
