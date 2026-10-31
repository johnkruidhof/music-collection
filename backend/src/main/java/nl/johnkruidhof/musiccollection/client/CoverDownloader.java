package nl.johnkruidhof.musiccollection.client;

import java.net.URI;
import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import nl.johnkruidhof.musiccollection.config.AppProperties;

/** Downloadt cover-afbeeldingen van Discogs/Cover Art Archive (redirects worden gevolgd). */
@Component
public class CoverDownloader {

    static final int MAX_BYTES = 15 * 1024 * 1024;

    private final RestClient restClient;
    private final RequestThrottle throttle;

    public CoverDownloader(RestClient coverDownloadRestClient, AppProperties appProperties) {
        this.restClient = coverDownloadRestClient;
        AppProperties.Covers covers = appProperties.covers();
        this.throttle = new RequestThrottle("Cover-download",
                new AppProperties.Throttle(covers.minInterval(), 1, covers.minInterval(), null));
    }

    /** @throws org.springframework.web.client.RestClientException bij een HTTP-/netwerkfout of geen afbeelding */
    public byte[] download(String url) {
        URI uri = URI.create(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new ResourceAccessException("Geen http(s)-URL: " + url);
        }
        ResponseEntity<byte[]> response = throttle.execute(() -> restClient.get().uri(uri).retrieve().toEntity(byte[].class));
        byte[] body = response.getBody();
        var contentType = response.getHeaders().getContentType();
        if (body == null || body.length == 0 || body.length > MAX_BYTES
                || (contentType != null && !"image".equals(contentType.getType()))) {
            throw new ResourceAccessException("Geen bruikbare afbeelding op " + url);
        }
        return body;
    }
}
