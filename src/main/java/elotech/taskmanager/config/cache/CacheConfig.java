package elotech.taskmanager.config.cache;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@Configuration
public class CacheConfig {

    public static final String PROJECT_SUMMARY_CACHE = "projectSummary";

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(PROJECT_SUMMARY_CACHE);
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean
    CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache,
                    @NonNull Object key) {
                log.warn("Cache GET error on cache='{}' key='{}'. Evicting key and continuing.",
                        cache.getName(),
                        Objects.toString(key),
                        exception);
                try {
                    cache.evict(key);
                } catch (RuntimeException evictException) {
                    log.warn("Cache EVICT after GET error also failed on cache='{}'.",
                            cache.getName(),
                            evictException);
                }
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache,
                    @NonNull Object key, @Nullable Object value) {
                log.warn("Cache PUT error on cache='{}' key='{}'. Ignoring and continuing.",
                        cache.getName(),
                        Objects.toString(key),
                        exception);
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache,
                    @NonNull Object key) {
                log.warn("Cache EVICT error on cache='{}' key='{}'. Ignoring and continuing.",
                        cache.getName(),
                        Objects.toString(key),
                        exception);
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.warn("Cache CLEAR error on cache='{}'. Ignoring and continuing.",
                        cache.getName(),
                        exception);
            }
        };
    }
}
