package nl.johnkruidhof.musiccollection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.discogs.DiscogsApi;

/** RestClient + {@link DiscogsApi}-proxy met token, User-Agent en time-outs uit {@code app.discogs}. */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class DiscogsClientConfig {

    @Bean
    DiscogsApi discogsApi(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        AppProperties.Discogs discogs = appProperties.discogs();
        if (!StringUtils.hasText(discogs.token())) {
            log.warn("DISCOGS_TOKEN is niet gezet: zoeken bij Discogs vereist een token en zal mislukken.");
        }
        return createDiscogsApi(restClientBuilder, discogs);
    }

    /** Ook gebruikt door de WireMock-tests, zodat die exact dezelfde client-configuratie testen. */
    public static DiscogsApi createDiscogsApi(RestClient.Builder restClientBuilder, AppProperties.Discogs discogs) {
        RestClient.Builder builder = HttpClientSupport.restClient(restClientBuilder, discogs.baseUrl(), discogs.userAgent());
        if (StringUtils.hasText(discogs.token())) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Discogs token=" + discogs.token());
        }
        return HttpClientSupport.httpService(builder.build(), DiscogsApi.class);
    }
}
