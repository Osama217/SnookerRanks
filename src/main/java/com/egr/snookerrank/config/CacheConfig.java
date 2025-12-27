package com.egr.snookerrank.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
@EnableCaching
public class CacheConfig {

    // Maximum number of entries per cache
    private static final int MAX_CACHE_SIZE = 100;

    @Bean
    public CacheManager cacheManager() {
        return new LoggingCacheManager("topPlayers", "orderOfMerit", "latestResult","rankingsMetaData","statsMetaData");
    }

    /** 
     * Custom CacheManager that logs cache operations
     */
    private static class LoggingCacheManager extends ConcurrentMapCacheManager {
        public LoggingCacheManager(String... cacheNames) {
            super(cacheNames);
        }

        @Override
        protected Cache createConcurrentMapCache(String name) {
            return new LoggingCache(name);
        }
    }

    /**
     * Custom Cache implementation that logs cache hits and misses with size limits
     * Uses LRU (Least Recently Used) eviction policy
     */
    private static class LoggingCache extends ConcurrentMapCache {
        private final Map<Object, Object> accessOrderMap;

        public LoggingCache(String name) {
            super(name, new ConcurrentHashMap<>(), true);
            // Maintain access order for LRU eviction
            this.accessOrderMap = Collections.synchronizedMap(
                new LinkedHashMap<Object, Object>(16, 0.75f, true)
            );
        }

        @Override
        public ValueWrapper get(Object key) {
            ValueWrapper value = super.get(key);
            // Update access order for LRU
            synchronized (accessOrderMap) {
                if (getNativeCache().containsKey(key)) {
                    accessOrderMap.put(key, key); // Touch to update order
                }
            }
            int currentSize = getNativeCache().size();
            if (value != null) {
                System.out.println("[CACHE] Cache HIT for key: " + key + " in cache: " + getName() + " (Size: " + currentSize + "/" + MAX_CACHE_SIZE + ")");
            } else {
                System.out.println("[CACHE] Cache MISS for key: " + key + " in cache: " + getName() + " - Fetching from DB (Size: " + currentSize + "/" + MAX_CACHE_SIZE + ")");
            }
            return value;
        }

        @Override
        public void put(Object key, Object value) {
            ConcurrentMap<Object, Object> store = getNativeCache();
            synchronized (accessOrderMap) {
                // Check if we need to evict before adding
                if (!store.containsKey(key) && store.size() >= MAX_CACHE_SIZE) {
                    // Evict the least recently used entry (oldest in accessOrderMap)
                    Object oldestKey = accessOrderMap.keySet().iterator().next();
                    store.remove(oldestKey);
                    accessOrderMap.remove(oldestKey);
                    System.out.println("[CACHE] Cache size limit reached (" + MAX_CACHE_SIZE + "). Evicting oldest entry: " + oldestKey + " from cache: " + getName());
                }
                // Add/update the entry
                store.put(key, value);
                accessOrderMap.put(key, key); // Update access order
            }
            int currentSize = store.size();
            System.out.println("[CACHE] Storing in cache for key: " + key + " in cache: " + getName() + " (Size: " + currentSize + "/" + MAX_CACHE_SIZE + ")");
        }

        @Override
        public void evict(Object key) {
            super.evict(key);
            synchronized (accessOrderMap) {
                accessOrderMap.remove(key);
            }
            int currentSize = getNativeCache().size();
            System.out.println("[CACHE] Evicted key: " + key + " from cache: " + getName() + " (Size: " + currentSize + "/" + MAX_CACHE_SIZE + ")");
        }

        @Override
        public void clear() {
            super.clear();
            synchronized (accessOrderMap) {
                accessOrderMap.clear();
            }
            System.out.println("[CACHE] Cleared all entries from cache: " + getName());
        }
    }
}

