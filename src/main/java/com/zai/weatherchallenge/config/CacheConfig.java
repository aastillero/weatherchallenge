package com.zai.weatherchallenge.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.weather.cache.ttl-seconds}")
    private int cacheTtlSeconds;

    @Value("${app.weather.cache.name}")
    private String cacheName;

    /**
     * Defines the primary CacheManager for the application.
     * By defining our own CacheManager bean, we override the Spring Boot default.
     * This allows us to enable async mode, which is required for caching
     * methods that return reactive types (Mono/Flux).
     *
     * @return A fully configured CaffeineCacheManager.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(cacheName);
        
        // **THE FIX**: Enable asynchronous cache operations
        cacheManager.setAsyncCacheMode(true); 

        // Configure the cache expiration policy programmatically
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS));
        
        return cacheManager;
    }
}