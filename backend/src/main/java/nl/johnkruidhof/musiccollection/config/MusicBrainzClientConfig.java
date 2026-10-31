package nl.johnkruidhof.musiccollection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import nl.johnkruidhof.musiccollection.client.musicbrainz.CoverArtArchiveApi;
import nl.johnkruidhof.musiccollection.client.musicbrainz.MusicBrainzApi;

/** RestClients voor MusicBrainz en het Cover Art Archive uit {@code app.musicbrainz} (geen token, wel User-Agent). */
@Configuration(proxyBeanMethods = false)
public class MusicBrainzClientConfig {

    @Bean
    MusicBrainzApi musicBrainzApi(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        return createMusicBrainzApi(restClientBuilder, appProperties.musicbrainz());
    }

    @Bean
    CoverArtArchiveApi coverArtArchiveApi(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        return createCoverArtArchiveApi(restClientBuilder, appProperties.musicbrainz());
    }

    /** Ook gebruikt door de WireMock-tests. */
    public static MusicBrainzApi createMusicBrainzApi(RestClient.Builder restClientBuilder,
            AppProperties.MusicBrainz musicbrainz) {
        RestClient restClient = HttpClientSupport
                .restClient(restClientBuilder, musicbrainz.baseUrl(), musicbrainz.userAgent()).build();
        return HttpClientSupport.httpService(restClient, MusicBrainzApi.class);
    }

    /** Ook gebruikt door de WireMock-tests. */
    public static CoverArtArchiveApi createCoverArtArchiveApi(RestClient.Builder restClientBuilder,
            AppProperties.MusicBrainz musicbrainz) {
        RestClient restClient = HttpClientSupport
                .restClient(restClientBuilder, musicbrainz.coverArtBaseUrl(), musicbrainz.userAgent()).build();
        return HttpClientSupport.httpService(restClient, CoverArtArchiveApi.class);
    }
}
