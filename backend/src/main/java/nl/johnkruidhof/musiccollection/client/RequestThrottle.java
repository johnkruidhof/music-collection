package nl.johnkruidhof.musiccollection.client;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import lombok.extern.slf4j.Slf4j;
import nl.johnkruidhof.musiccollection.config.AppProperties;

/**
 * Rate limiting en retry voor één externe bron. Requests krijgen om de beurt een tijdslot
 * ({@code minInterval} uit elkaar); de aanroepende (virtuele) thread wacht daarop. Tijdelijke fouten
 * (429, 502, 503, 504, netwerk) worden opnieuw geprobeerd met oplopende wachttijd.
 */
@Slf4j
public class RequestThrottle {

    private final String name;
    private final AppProperties.Throttle settings;
    private long nextSlotNanos = System.nanoTime();

    public RequestThrottle(String name, AppProperties.Throttle settings) {
        this.name = name;
        this.settings = settings;
    }

    public <T> T execute(Supplier<T> request) {
        for (int attempt = 0; ; attempt++) {
            acquireSlot();
            try {
                return request.get();
            } catch (RestClientException ex) {
                if (attempt >= settings.maxRetries() || !isRetryable(ex)) {
                    throw ex;
                }
                Duration backoff = settings.retryBackoff().multipliedBy(1L << attempt);
                log.info("{}: tijdelijke fout ({}), nieuwe poging over {} ms", name, ex.getMessage(), backoff.toMillis());
                sleep(backoff);
            }
        }
    }

    static boolean isRetryable(RestClientException ex) {
        return ex instanceof HttpClientErrorException.TooManyRequests
                || ex instanceof HttpServerErrorException.ServiceUnavailable
                || ex instanceof HttpServerErrorException.BadGateway
                || ex instanceof HttpServerErrorException.GatewayTimeout
                || ex instanceof ResourceAccessException;
    }

    private void acquireSlot() {
        long waitNanos;
        synchronized (this) {
            long now = System.nanoTime();
            long slot = Math.max(now, nextSlotNanos);
            waitNanos = slot - now;
            if (waitNanos > settings.maxWait().toNanos()) {
                throw new ResourceAccessException(name + ": te veel verzoeken in de wachtrij");
            }
            nextSlotNanos = slot + settings.minInterval().toNanos();
        }
        if (waitNanos > 0) {
            sleep(Duration.ofNanos(waitNanos));
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResourceAccessException("Onderbroken tijdens wachten op externe bron");
        }
    }
}
