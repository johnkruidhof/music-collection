package nl.johnkruidhof.musiccollection.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemSpecifications;

/**
 * Export van de eigen collectie als CSV (F-22): puntkomma als scheidingsteken en een decimale komma,
 * zodat het bestand direct goed opent in een Nederlandse Excel; UTF-8 met BOM. Type en staat als
 * code (bv. {@code VINYL}, {@code NEAR_MINT}).
 */
@Service
@RequiredArgsConstructor
public class CollectionExportService {

    static final char SEPARATOR = ';';
    static final List<String> HEADER = List.of("Artiest", "Titel", "Jaar", "Type", "Formaat", "Label",
            "Catalogusnummer", "Land", "Barcode", "Bron", "Discogs-id", "MusicBrainz-id", "Staat plaat",
            "Staat hoes", "Aankoopdatum", "Aankoopprijs", "Gekocht bij", "Opslaglocatie", "Notities", "Toegevoegd",
            "Genre", "Subgenre");
    private static final DateTimeFormatter ADDED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.of("Europe/Amsterdam"));

    private final CollectionItemRepository collectionItemRepository;
    private final UserService userService;

    @Transactional
    public String exportCsv(Jwt jwt) {
        List<CollectionItem> items = collectionItemRepository.findAll(
                CollectionItemSpecifications.ownedBy(userService.getOrCreate(jwt).getId()),
                Sort.by("release.artist", "release.title", "addedAt"));
        return '﻿' + Csv.write(HEADER, items, CollectionExportService::columns, SEPARATOR);
    }

    private static List<String> columns(CollectionItem item) {
        Release r = item.getRelease();
        return List.of(
                text(r.getArtist()), text(r.getTitle()), text(r.getReleaseYear()), text(r.getMediaType()),
                text(r.getFormatDetails()), text(r.getLabel()), text(r.getCatalogNumber()), text(r.getCountry()),
                text(r.getBarcode()), text(r.getSource()), text(r.getDiscogsId()), text(r.getMusicbrainzId()),
                text(item.getConditionMedia()), text(item.getConditionSleeve()), text(item.getPurchaseDate()),
                price(item.getPurchasePrice()), text(item.getPurchaseLocation()), text(item.getStorageLocation()),
                text(item.getNotes()), item.getAddedAt() == null ? "" : ADDED_AT.format(item.getAddedAt()),
                item.getGenre() == null ? "" : item.getGenre().topLevel().getName(),
                item.getGenre() != null && item.getGenre().isSubgenre() ? item.getGenre().getName() : "");
    }

    private static String text(Object value) {
        return Objects.toString(value, "");
    }

    private static String price(BigDecimal price) {
        return price == null ? "" : price.toPlainString().replace('.', ',');
    }

    public String fileName(Instant now) {
        return "muziekcollectie-" + DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("Europe/Amsterdam")).format(now)
                + ".csv";
    }
}
