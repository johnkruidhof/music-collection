package nl.johnkruidhof.musiccollection.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** Gedeelde opbouw van de RestClients voor externe bronnen: time-outs, redirects en User-Agent. */
final class HttpClientSupport {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    private HttpClientSupport() {
    }

    /** RestClient-builder met (optioneel) base-URL, User-Agent en time-outs; volgt redirects (Cover Art Archive). */
    static RestClient.Builder restClient(RestClient.Builder builder, String baseUrl, String userAgent) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        RestClient.Builder configured = builder.clone()
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent);
        return baseUrl == null ? configured : configured.baseUrl(baseUrl);
    }

    static <T> T httpService(RestClient restClient, Class<T> serviceType) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(serviceType);
    }
}
