package nl.johnkruidhof.musiccollection.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Caffeine-cache; instellingen in application.yml (spring.cache.*). */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    public static final String BARCODE_LOOKUP_CACHE = "barcodeLookup";
    public static final String SEARCH_CACHE = "search";
    public static final String RELEASE_DETAILS_CACHE = "releaseDetails";
}
