package com.zai.weatherchallenge.controller;

import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.exception.AllProvidersDownException;
import com.zai.weatherchallenge.service.WeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/v1/weather")
@Slf4j
public class WeatherController {

    private final WeatherService weatherService;
    private final CacheManager cacheManager;
    private final String cacheName;
    private final String city;

    public WeatherController(WeatherService weatherService,
                             CacheManager cacheManager,
                             @Value("${app.weather.cache.name}") String cacheName,
                             @Value("${app.weather.city}") String city) {
        this.weatherService = weatherService;
        this.cacheManager = cacheManager;
        this.cacheName = cacheName;
        this.city = city;
    }

    @GetMapping("/melbourne")
    public Mono<ResponseEntity<Object>> getMelbourneWeather() {
        return weatherService.getWeatherForCity()
            .<ResponseEntity<Object>>map(ResponseEntity::ok)
            .onErrorResume(AllProvidersDownException.class, e -> {
                log.warn("All providers are down. Attempting to serve stale data from cache.");

                ResponseEntity<Object> fallbackResponse = findStaleFromCache()
                    .<ResponseEntity<Object>>map(staleResponse -> {
                        log.info("Serving stale weather data from cache.");
                        return ResponseEntity.ok().body(staleResponse);
                    })
                    .orElseGet(() -> {
                        log.error("Could not find any data in cache. Service is completely unavailable.");
                        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body("Weather service is temporarily unavailable. Please try again later.");
                    });
                
                return Mono.just(fallbackResponse);
            });
    }

    private Optional<WeatherResponse> findStaleFromCache() {
        return Optional.ofNullable(cacheManager.getCache(cacheName))
            .map(cache -> cache.get(city, Mono.class)) // Spring Cache stores reactive types wrapped
            .flatMap(cachedMono -> {
                // The cache might contain a Mono that resolves to null or an error.
                // We block here briefly as this is a last-resort scenario.
                try {
                    return Optional.ofNullable((WeatherResponse) cachedMono.block());
                } catch (Exception e) {
                    return Optional.empty();
                }
            });
    }
}