package com.zai.weatherchallenge.service;

import com.zai.weatherchallenge.client.WeatherProviderClient;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.exception.AllProvidersDownException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class WeatherService {

    private final WeatherProviderClient primaryClient;
    private final WeatherProviderClient failoverClient;
    private final String city;
    private final String cacheName;

    public WeatherService(
        @Qualifier("primary") WeatherProviderClient primaryClient,
        @Qualifier("failover") WeatherProviderClient failoverClient,
        @Value("${app.weather.city}") String city,
        @Value("${app.weather.cache.name}") String cacheName) {
        this.primaryClient = primaryClient;
        this.failoverClient = failoverClient;
        this.city = city;
        this.cacheName = cacheName;
    }

    //@Cacheable(value = "${app.weather.cache.name}", key = "#root.target.city")
    @Cacheable(value = "weather-cache", key = "#root.target.city")
    public Mono<WeatherResponse> getWeatherForCity() {
        log.info("Cache miss. Fetching fresh weather data for {}.", city);
        return primaryClient.fetchWeather(city)
            .doOnSuccess(response -> log.info("Successfully fetched weather from primary provider: {}", primaryClient.getProviderName()))
            .onErrorResume(e -> {
                log.warn("Primary provider ({}) failed: {}. Attempting failover.", primaryClient.getProviderName(), e.toString());
                return failoverClient.fetchWeather(city)
                    .doOnSuccess(response -> log.info("Successfully fetched weather from failover provider: {}", failoverClient.getProviderName()))
                    .onErrorMap(failoverException -> {
                        log.error("All weather providers are down. Primary failed and failover also failed: {}", failoverException.getMessage());
                        return new AllProvidersDownException("All weather providers are currently unavailable.", failoverException);
                    });
            });
    }

    // Public getter for the SpEL expression in @Cacheable
    public String getCity() {
        return city;
    }
}