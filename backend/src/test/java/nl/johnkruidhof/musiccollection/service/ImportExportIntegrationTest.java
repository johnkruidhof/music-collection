package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import nl.johnkruidhof.musiccollection.TestDatabase;
import nl.johnkruidhof.musiccollection.TestcontainersConfiguration;
import nl.johnkruidhof.musiccollection.dto.collection.AddToCollectionRequest;
import nl.johnkruidhof.musiccollection.dto.collection.CollectionItemFields;
import nl.johnkruidhof.musiccollection.dto.importexport.ImportJobDto;
import nl.johnkruidhof.musiccollection.dto.release.ManualReleaseRequest;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.CollectionItem;
import nl.johnkruidhof.musiccollection.entity.MediaCondition;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.repository.AppUserRepository;
import nl.johnkruidhof.musiccollection.repository.CollectionItemRepository;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ImportExportIntegrationTest {

    private static final Jwt ALICE = jwt("alice-sub", "alice");

    private static final String EXPORT = """
            Catalog#,Artist,Title,Label,Format,Rating,Released,release_id,CollectionFolder,Date Added,Collection Media Condition,Collection Sleeve Condition,Collection Notes
            NODATA 02,Radiohead,OK Computer,Parlophone,LP,,1997,1,Uncategorized,2023-04-12 10:11:12,Near Mint (NM or M-),,Eerste
            NODATA 02,Radiohead,OK Computer,Parlophone,LP,,1997,1,Uncategorized,2023-04-12 10:11:12,Good (G),,Tweede
            X,Onbekend,Bestaat niet,X,LP,,1990,999,Uncategorized,2023-04-12 10:11:12,,,
            """;

    @Autowired
    private ImportService importService;

    @Autowired
    private CollectionExportService exportService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private CollectionItemRepository collectionItemRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ExternalLookupService externalLookupService;

    @BeforeEach
    void setUp() {
        TestDatabase.clean(jdbc);
        given(externalLookupService.getDetails(eq(MetadataSource.DISCOGS), anyString())).willReturn(Optional.empty());
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(Optional.of(new ReleaseDto(
                null, null, "Radiohead", "OK Computer", 1997, MediaType.VINYL, null, "Parlophone", "NODATA 02", "UK",
                null, null, MetadataSource.DISCOGS, "1", null, List.of())));
    }

    @Test
    void discogsExportIsImportedAndReimportDoesNotDuplicate() throws InterruptedException {
        ImportJobDto first = awaitFinished(importService.startDiscogsImport(ALICE, bytes(EXPORT)));

        assertThat(first.status()).isEqualTo(ImportJobDto.Status.FINISHED);
        assertThat(first).extracting("total", "processed", "imported", "skipped", "failed")
                .containsExactly(3, 3, 2, 0, 1);
        assertThat(first.errors()).singleElement().asString().contains("regel 4").contains("niet gevonden");
        List<CollectionItem> items = collectionItemRepository.findAll();
        assertThat(items).extracting(CollectionItem::getConditionMedia)
                .containsExactlyInAnyOrder(MediaCondition.NEAR_MINT, MediaCondition.GOOD);

        ImportJobDto second = awaitFinished(importService.startDiscogsImport(ALICE, bytes(EXPORT)));

        assertThat(second).extracting("imported", "skipped", "failed").containsExactly(0, 2, 1);
        assertThat(collectionItemRepository.count()).isEqualTo(2);
        assertThat(importService.latest(ALICE).id()).isEqualTo(second.id());
    }

    @Test
    void invalidFileIsRejectedImmediately() {
        assertThatThrownBy(() -> importService.startDiscogsImport(ALICE, bytes("Artiest;Titel\nX;Y")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void exportContainsOwnItemsInDutchExcelFormat() {
        collectionService.add(ALICE, new AddToCollectionRequest(null, null,
                new ManualReleaseRequest(null, "De Dijk", "Nooit meer slapen; live", 1985, MediaType.VINYL, null, null,
                        null, null, List.of()),
                new CollectionItemFields(MediaCondition.VERY_GOOD, null, null, new BigDecimal("12.50"), null,
                        "Kast A", "Met \"handtekening\"", null)));

        String csv = exportService.exportCsv(ALICE);

        assertThat(csv).startsWith("﻿Artiest;Titel;Jaar;Type;");
        List<List<String>> rows = Csv.parse(csv, ';');
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsSubsequence("De Dijk", "Nooit meer slapen; live", "1985", "VINYL");
        assertThat(rows.get(1)).contains("VERY_GOOD", "12,50", "Kast A", "Met \"handtekening\"", "MANUAL");
    }

    private ImportJobDto awaitFinished(ImportJobDto started) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        ImportJobDto job = started;
        while (job.status() == ImportJobDto.Status.RUNNING && System.nanoTime() < deadline) {
            Thread.sleep(50);
            job = importService.get(ALICE, started.id());
        }
        return job;
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static Jwt jwt(String subject, String username) {
        return Jwt.withTokenValue("token-" + UUID.randomUUID()).header("alg", "none")
                .subject(subject).claim("preferred_username", username).build();
    }
}
