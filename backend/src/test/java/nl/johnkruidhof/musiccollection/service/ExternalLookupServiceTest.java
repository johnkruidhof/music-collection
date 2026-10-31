package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.johnkruidhof.musiccollection.client.MetadataProvider;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;

class ExternalLookupServiceTest {

    private final MetadataProvider discogs = mock(MetadataProvider.class);
    private final MetadataProvider musicbrainz = mock(MetadataProvider.class);
    private ExternalLookupService service;

    @BeforeEach
    void setUp() {
        given(discogs.source()).willReturn(MetadataSource.DISCOGS);
        given(musicbrainz.source()).willReturn(MetadataSource.MUSICBRAINZ);
        service = new ExternalLookupService(List.of(discogs, musicbrainz));
    }

    @Test
    void discogsResultIsUsedWithoutAskingMusicBrainz() {
        given(discogs.findByBarcode("724385522925")).willReturn(List.of(candidate(MetadataSource.DISCOGS, "1")));

        assertThat(service.findByBarcode("724385522925")).extracting(ReleaseCandidateDto::externalId).containsExactly("1");
        verify(musicbrainz, never()).findByBarcode("724385522925");
    }

    @Test
    void ean13VariantIsTriedBeforeFallingBackToMusicBrainz() {
        given(discogs.findByBarcode("724385522925")).willReturn(List.of());
        given(discogs.findByBarcode("0724385522925")).willReturn(List.of(candidate(MetadataSource.DISCOGS, "2")));

        assertThat(service.findByBarcode("724385522925")).extracting(ReleaseCandidateDto::externalId).containsExactly("2");
        verify(musicbrainz, never()).findByBarcode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void musicBrainzIsFallbackWhenDiscogsFindsNothing() {
        given(discogs.findByBarcode(org.mockito.ArgumentMatchers.anyString())).willReturn(List.of());
        given(musicbrainz.findByBarcode("724385522925")).willReturn(List.of(candidate(MetadataSource.MUSICBRAINZ, "mb")));

        assertThat(service.findByBarcode("724385522925")).extracting(ReleaseCandidateDto::source)
                .containsExactly(MetadataSource.MUSICBRAINZ);
    }

    @Test
    void musicBrainzIsFallbackWhenDiscogsFails() {
        given(discogs.search(org.mockito.ArgumentMatchers.any())).willThrow(failure("Discogs"));
        given(musicbrainz.search(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(candidate(MetadataSource.MUSICBRAINZ, "mb")));

        assertThat(service.search(criteria())).hasSize(1);
    }

    @Test
    void failureIsReportedWhenNoSourceHasResults() {
        given(discogs.search(org.mockito.ArgumentMatchers.any())).willThrow(failure("Discogs"));
        given(musicbrainz.search(org.mockito.ArgumentMatchers.any())).willReturn(List.of());

        // Geen leeg resultaat (dat zou gecachet worden), maar de fout van Discogs.
        assertThatThrownBy(() -> service.search(criteria()))
                .isInstanceOf(ExternalLookupException.class)
                .extracting("source").isEqualTo("Discogs");
    }

    @Test
    void emptyResultWhenAllSourcesAnswerWithNothing() {
        given(discogs.search(org.mockito.ArgumentMatchers.any())).willReturn(List.of());
        given(musicbrainz.search(org.mockito.ArgumentMatchers.any())).willReturn(List.of());

        assertThat(service.search(criteria())).isEmpty();
    }

    @Test
    void detailsComeFromTheRequestedSource() {
        ReleaseDto release = new ReleaseDto(null, null, "Radiohead", "OK Computer", 1997, MediaType.CD, null, null,
                null, null, null, null, MetadataSource.MUSICBRAINZ, null, "mbid", List.of());
        given(musicbrainz.getDetails("mbid")).willReturn(Optional.of(release));

        assertThat(service.getDetails(MetadataSource.MUSICBRAINZ, "mbid")).contains(release);
        verify(discogs, never()).getDetails("mbid");
        assertThat(service.getDetails(MetadataSource.MANUAL, "x")).isEmpty();
    }

    private static ReleaseSearchCriteria criteria() {
        return new ReleaseSearchCriteria("Radiohead", "OK Computer", null, null);
    }

    private static ExternalLookupException failure(String source) {
        return new ExternalLookupException(source, source + " plat", null);
    }

    static ReleaseCandidateDto candidate(MetadataSource source, String externalId) {
        return new ReleaseCandidateDto(source, externalId, "Radiohead", "OK Computer", 1997, MediaType.CD,
                "CD, Album", "Parlophone", "NODATA 02", "UK", "724385522925", null, null, null);
    }
}
