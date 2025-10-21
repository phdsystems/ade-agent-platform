package dev.adeengineer.platform.config;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache configuration using Caffeine for LLM response caching.
 *
 * <p>Reduces redundant API calls for repeated prompts.
 */
@Slf4j
@Configuration
@EnableCaching
public final class CacheConfig {

    /** Whether LLM response caching is enabled. */
    @Value("${llm.cache.enabled:true}")
    private boolean cacheEnabled;

    /** Cache time-to-live in minutes. */
    @Value("${llm.cache.ttl-minutes:60}")
    private int ttlMinutes;

    /** Maximum number of cached entries. */
    @Value("${llm.cache.max-size:1000}")
    private int maxSize;

    /**
     * Configure cache manager for LLM responses.
     *
     * @return Configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        if (!cacheEnabled) {
            log.info("LLM response caching is DISABLED");
            return new CaffeineCacheManager();
        }

        CaffeineCacheManager manager = new CaffeineCacheManager("llmResponses");
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                        .maximumSize(maxSize)
                        .recordStats());

        log.info("LLM response caching ENABLED: " + "TTL={}min, MaxSize={}", ttlMinutes, maxSize);

        return manager;
    }
}
