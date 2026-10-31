package nl.johnkruidhof.musiccollection.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import nl.johnkruidhof.musiccollection.client.MetadataProvider;
import nl.johnkruidhof.musiccollection.config.CacheConfig;
import nl.johnkruidhof.musiccollection.entity.MetadataSource;
import nl.johnkruidhof.musiccollection.exception.ExternalLookupException;

/** Controleert dat resultaten gecachet worden, maar fouten niet. */
@SpringJUnitConfig
class ExternalLookupServiceCacheTest {

    @Configuration(proxyBeanMethods = false)
    @Import({CacheConfig.class, ExternalLookupService.class})
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager();
        }

        @Bean
        MetadataProvider provider() {
            MetadataProvider provider = mock(MetadataProvider.class);
            given(provider.source()).willReturn(MetadataSource.DISCOGS);
            return provider;
        }
    }

    @Autowired
    private ExternalLookupService service;

    @Autowired
    private MetadataProvider provider;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(provider);
        given(provider.source()).willReturn(MetadataSource.DISCOGS);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void barcodeResultIsCached() {
        given(provider.findByBarcode("5099749129829"))
                .willReturn(List.of(ExternalLookupServiceTest.candidate(MetadataSource.DISCOGS, "1")));

        service.findByBarcode("5099749129829");
        service.findByBarcode("5099749129829");

        verify(provider, times(1)).findByBarcode("5099749129829");
    }

    @Test
    void failureIsNotCached() {
        given(provider.findByBarcode(anyString())).willThrow(new ExternalLookupException("Discogs", "plat", null));

        assertThatThrownBy(() -> service.findByBarcode("5099749129829")).isInstanceOf(ExternalLookupException.class);
        assertThatThrownBy(() -> service.findByBarcode("5099749129829")).isInstanceOf(ExternalLookupException.class);

        verify(provider, times(2)).findByBarcode("5099749129829");
    }
}
