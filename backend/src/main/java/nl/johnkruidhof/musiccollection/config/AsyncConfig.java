package nl.johnkruidhof.musiccollection.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Achtergrondtaken: {@code @Async} (cover cachen na import, F-21; import, F-22) en {@code @Scheduled}
 * (cover-backfill). Met {@code spring.threads.virtual.enabled} draaien die op virtuele threads.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
