package nl.johnkruidhof.musiccollection.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import nl.johnkruidhof.musiccollection.dto.wishlist.WishlistItemDto;
import nl.johnkruidhof.musiccollection.entity.WishlistItem;

@Mapper(uses = CollectionItemMapper.class)
public interface WishlistMapper {

    @Mapping(target = "releaseId", source = "release.id")
    @Mapping(target = "artist", source = "release.artist")
    @Mapping(target = "title", source = "release.title")
    @Mapping(target = "releaseYear", source = "release.releaseYear")
    @Mapping(target = "mediaType", source = "release.mediaType")
    @Mapping(target = "formatDetails", source = "release.formatDetails")
    @Mapping(target = "label", source = "release.label")
    @Mapping(target = "country", source = "release.country")
    @Mapping(target = "coverThumbnailUrl", source = "release.coverThumbnailUrl")
    @Mapping(target = "source", source = "release.source")
    @Mapping(target = "discogsId", source = "release.discogsId")
    @Mapping(target = "musicbrainzId", source = "release.musicbrainzId")
    @Mapping(target = "genreId", source = "genre.id")
    @Mapping(target = "genreName", source = "genre", qualifiedByName = "genreName")
    @Mapping(target = "subgenreName", source = "genre", qualifiedByName = "subgenreName")
    WishlistItemDto toDto(WishlistItem item);
}
