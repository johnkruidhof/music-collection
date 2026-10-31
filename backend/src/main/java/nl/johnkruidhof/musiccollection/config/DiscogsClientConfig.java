package nl.johnkruidhof.musiccollection.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.client.discogs.DiscogsApi;

/** RestClient + {@link DiscogsApi}-proxy met token, User-Agent en time-outs uit {@code app.discogs}. */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class DiscogsClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

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
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        RestClient restClient = restClientBuilder.clone()
                .baseUrl(discogs.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    headers.set(HttpHeaders.USER_AGENT, discogs.userAgent());
                    if (StringUtils.hasText(discogs.token())) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Discogs token=" + discogs.token());
                    }
                })
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(DiscogsApi.class);
    }
}
