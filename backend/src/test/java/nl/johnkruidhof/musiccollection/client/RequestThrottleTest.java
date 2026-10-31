package nl.johnkruidhof.musiccollection.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import nl.johnkruidhof.musiccollection.config.AppProperties;

class RequestThrottleTest {

    @Test
    void requestsAreSpacedByMinInterval() {
        RequestThrottle throttle = new RequestThrottle("test",
                new AppProperties.Throttle(Duration.ofMillis(60), 0, Duration.ZERO, Duration.ofSeconds(5)));

        long start = System.nanoTime();
        for (int i = 0; i < 3; i++) {
            throttle.execute(() -> "ok");
        }

        // Eerste request direct, daarna 2× 60 ms wachten.
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(120));
    }

    @Test
    void retryableErrorIsRetriedUntilSuccess() {
        RequestThrottle throttle = new RequestThrottle("test", retries(2));
        AtomicInteger calls = new AtomicInteger();

        String result = throttle.execute(() -> {
            if (calls.incrementAndGet() < 3) {
                throw HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "busy", null, null, null);
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }

    @Test
    void retriesAreLimited() {
        RequestThrottle throttle = new RequestThrottle("test", retries(1));
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> throttle.execute(() -> {
            calls.incrementAndGet();
            throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "slow down", null, null, null);
        })).isInstanceOf(HttpClientErrorException.TooManyRequests.class);
        assertThat(calls).hasValue(2);
    }

    @Test
    void clientErrorIsNotRetried() {
        RequestThrottle throttle = new RequestThrottle("test", retries(3));
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> throttle.execute(() -> {
            calls.incrementAndGet();
            throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "nope", null, null, null);
        })).isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(calls).hasValue(1);
    }

    @Test
    void tooLongQueueFailsFast() {
        RequestThrottle throttle = new RequestThrottle("test",
                new AppProperties.Throttle(Duration.ofSeconds(10), 0, Duration.ZERO, Duration.ofSeconds(1)));
        throttle.execute(() -> "eerste");

        // Het volgende slot is pas over 10 s; dat is langer dan maxWait.
        assertThatThrownBy(() -> throttle.execute(() -> "tweede"))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("wachtrij");
    }

    private static AppProperties.Throttle retries(int maxRetries) {
        return new AppProperties.Throttle(Duration.ZERO, maxRetries, Duration.ZERO, Duration.ofSeconds(1));
    }
}
