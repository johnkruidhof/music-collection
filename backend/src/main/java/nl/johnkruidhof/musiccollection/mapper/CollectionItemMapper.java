package nl.johnkruidhof.musiccollection.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemDto;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemSummaryDto;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.Genre;

@Mapper(uses = ReleaseMapper.class)
public interface CollectionItemMapper {

    @Mapping(target = "genreId", source = "genre.id")
    @Mapping(target = "genreName", source = "genre", qualifiedByName = "genreName")
    @Mapping(target = "subgenreName", source = "genre", qualifiedByName = "subgenreName")
    CollectionItemDto toDto(CollectionItem item);

    @Mapping(target = "releaseId", source = "release.id")
    @Mapping(target = "artist", source = "release.artist")
    @Mapping(target = "title", source = "release.title")
    @Mapping(target = "releaseYear", source = "release.releaseYear")
    @Mapping(target = "mediaType", source = "release.mediaType")
    @Mapping(target = "formatDetails", source = "release.formatDetails")
    @Mapping(target = "coverThumbnailUrl", source = "release.coverThumbnailUrl")
    @Mapping(target = "genreId", source = "genre.id")
    @Mapping(target = "genreName", source = "genre", qualifiedByName = "genreName")
    @Mapping(target = "subgenreName", source = "genre", qualifiedByName = "subgenreName")
    CollectionItemSummaryDto toSummary(CollectionItem item);

    /** Overschrijft alle persoonlijke velden (ook met {@code null}). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "release", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "genre", ignore = true)
    void applyFields(CollectionItemFields fields, @MappingTarget CollectionItem item);

    /** Hoofdgenre (ook als het item een subgenre heeft). */
    @Named("genreName")
    default String genreName(Genre genre) {
        return genre == null ? null : genre.topLevel().getName();
    }

    @Named("subgenreName")
    default String subgenreName(Genre genre) {
        return genre != null && genre.isSubgenre() ? genre.getName() : null;
    }
}
