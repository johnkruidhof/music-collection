package nl.johnkruidhof.musiccollection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** RestClient voor het downloaden van covers (geen base-URL; User-Agent van MusicBrainz). */
@Configuration(proxyBeanMethods = false)
public class CoverDownloadConfig {

    @Bean
    RestClient coverDownloadRestClient(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        return HttpClientSupport.restClient(restClientBuilder, null, appProperties.musicbrainz().userAgent()).build();
    }
}
