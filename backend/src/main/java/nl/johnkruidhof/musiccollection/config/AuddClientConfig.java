package nl.johnkruidhof.musiccollection.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/** RestClient voor AudD (F-30); langere lees-time-out omdat fingerprinten een paar seconden kan duren. */
@Slf4j
@Configuration(proxyBeanMethods = false)
public class AuddClientConfig {

    @Bean
    RestClient auddRestClient(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        if (!appProperties.audd().enabled()) {
            log.info("AUDD_API_TOKEN is niet gezet: luisteren (muziekherkenning) staat uit.");
        }
        return createAuddRestClient(restClientBuilder, appProperties);
    }

    /** Ook gebruikt door de WireMock-tests. */
    public static RestClient createAuddRestClient(RestClient.Builder restClientBuilder, AppProperties appProperties) {
        RestClient.Builder builder = HttpClientSupport.restClient(restClientBuilder, appProperties.audd().baseUrl(),
                appProperties.musicbrainz().userAgent());
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                // HTTP/1.1, zoals in HttpClientSupport (geen h2c-upgrade op een POST met body).
                java.net.http.HttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        return builder.requestFactory(requestFactory).build();
    }
}
