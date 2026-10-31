package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseCandidateDto;
import nl.johnkruidhof.musiccollection.dto.lookup.ReleaseSearchCriteria;
import nl.johnkruidhof.musiccollection.dto.release.ReleaseDto;
import nl.johnkruidhof.musiccollection.entity.MediaType;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.entity.Release;
import nl.johnkruidhof.musiccollection.entity.Track;
import nl.johnkruidhof.musiccollection.exception.InvalidRequestException;
import nl.johnkruidhof.musiccollection.exception.ResourceNotFoundException;
import nl.johnkruidhof.musiccollection.mapper.ReleaseMapperImpl;
import nl.johnkruidhof.musiccollection.repository.ReleaseRepository;

class LookupServiceTest {

    private final ReleaseRepository releaseRepository = mock(ReleaseRepository.class);
    private final ExternalLookupService externalLookupService = mock(ExternalLookupService.class);
    private final LookupService service = new LookupService(releaseRepository, externalLookupService,
            new ReleaseMapperImpl());

    @Test
    void barcodeIsNormalizedAndLocalCatalogComesFirst() {
        given(releaseRepository.findByBarcodeInAndSourceNot(List.of("724385522925", "0724385522925"),
                MetadataSource.MANUAL)).willReturn(List.of(discogsRelease()));

        List<ReleaseCandidateDto> candidates = service.lookupBarcode("7 24385-52292 5");

        assertThat(candidates).singleElement().satisfies(c -> {
            assertThat(c.source()).isEqualTo(MetadataSource.DISCOGS);
            assertThat(c.externalId()).isEqualTo("2149389");
            assertThat(c.title()).isEqualTo("OK Computer");
        });
        verifyNoInteractions(externalLookupService);
    }

    @Test
    void externalSourcesAreAskedWhenNotInLocalCatalog() {
        given(releaseRepository.findByBarcodeInAndSourceNot(any(), any())).willReturn(List.of());
        given(externalLookupService.findByBarcode("724385522925"))
                .willReturn(List.of(ExternalLookupServiceTest.candidate(MetadataSource.MUSICBRAINZ, "mb")));

        assertThat(service.lookupBarcode("724385522925")).hasSize(1);
    }

    @Test
    void invalidBarcodeIsRejected() {
        assertThatThrownBy(() -> service.lookupBarcode("abc")).isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(releaseRepository, externalLookupService);
    }

    @Test
    void searchNeedsArtistOrTitleAndStripsInput() {
        assertThatThrownBy(() -> service.search(new ReleaseSearchCriteria(" ", null, 1997, null)))
                .isInstanceOf(InvalidRequestException.class);

        service.search(new ReleaseSearchCriteria("  Radiohead ", " ", 1997, MediaType.VINYL));
        verify(externalLookupService).search(new ReleaseSearchCriteria("Radiohead", null, 1997, MediaType.VINYL));
    }

    @Test
    void detailsFromLocalCatalogIncludeIdAndTracks() {
        Release local = discogsRelease();
        local.addTrack(new Track(1, 1, "A1", "Airbag"));
        given(releaseRepository.findByDiscogsId("2149389")).willReturn(Optional.of(local));

        ReleaseDto release = service.getDetails("discogs", "2149389");

        assertThat(release.title()).isEqualTo("OK Computer");
        assertThat(release.tracks()).extracting("title").containsExactly("Airbag");
        verify(externalLookupService, never()).getDetails(any(), anyString());
    }

    @Test
    void detailsFromExternalSourceWhenNotLocal() {
        ReleaseDto external = new ReleaseDto(null, null, "Radiohead", "OK Computer", 1997, MediaType.CD, null, null,
                null, null, null, null, MetadataSource.MUSICBRAINZ, null, "mbid", List.of());
        given(releaseRepository.findByMusicbrainzId("mbid")).willReturn(Optional.empty());
        given(externalLookupService.getDetails(MetadataSource.MUSICBRAINZ, "mbid")).willReturn(Optional.of(external));

        assertThat(service.getDetails("MusicBrainz", "mbid")).isEqualTo(external);
    }

    @Test
    void unknownReleaseIsNotFound() {
        given(releaseRepository.findByDiscogsId("1")).willReturn(Optional.empty());
        given(externalLookupService.getDetails(MetadataSource.DISCOGS, "1")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetails("discogs", "1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unknownOrManualSourceIsRejected() {
        assertThatThrownBy(() -> service.getDetails("spotify", "1")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.getDetails("manual", "1")).isInstanceOf(InvalidRequestException.class);
    }

    private static Release discogsRelease() {
        Release release = new Release("Radiohead", "OK Computer", MediaType.VINYL, MetadataSource.DISCOGS);
        release.setDiscogsId("2149389");
        release.setBarcode("724385522925");
        return release;
    }
}
