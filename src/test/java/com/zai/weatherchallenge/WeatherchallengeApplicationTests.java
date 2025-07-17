package com.zai.weatherchallenge;

import com.zai.weatherchallenge.client.WeatherProviderClient;
import com.zai.weatherchallenge.controller.WeatherController;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.exception.AllProvidersDownException;
import com.zai.weatherchallenge.service.WeatherService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import org.springframework.cache.Cache;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.Collections;


@WebFluxTest(controllers = WeatherController.class)
@Import(WeatherchallengeApplicationTests.TestCacheConfiguration.class)
class WeatherchallengeApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    // A nested configuration class to provide a mock CacheManager for tests
    static class TestCacheConfiguration {
        @Bean
        CacheManager cacheManager() {
            SimpleCacheManager cacheManager = new SimpleCacheManager();
            cacheManager.setCaches(Collections.singletonList(
                    new ConcurrentMapCache("weather-cache")
            ));
            return cacheManager;
        }
    }

    @MockitoBean(name = "primaryWeatherProvider")
    private WeatherProviderClient primaryClient;

    @MockitoBean(name = "failoverWeatherProvider")
    private WeatherProviderClient failoverClient;

    // **THE FIX**: Mock the WeatherService bean directly
    @MockitoBean
    private WeatherService weatherService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear cache before each test to ensure a clean state
        cacheManager.getCache("weather-cache").clear();
        // Reset mocks to clear any previous interactions
        reset(primaryClient, failoverClient);
    }

    @Test
    void whenGetWeather_andServiceSucceeds_shouldReturnOk() {
        // Given
        WeatherResponse weatherResponse = WeatherResponse.builder()
                .temperatureDegrees(20.0)
                .windSpeed(15.0)
                .build();

        // Mock the service layer's behavior
        when(weatherService.getWeatherForCity()).thenReturn(Mono.just(weatherResponse));

        // When & Then
        webTestClient.get().uri("/v1/weather/melbourne")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeatherResponse.class).isEqualTo(weatherResponse);
    }

    // NOTE: The Caching test is now much simpler as we don't need Thread.sleep.
    // However, testing caching this way is an integration detail.
    // The most important test is the stale-on-error behavior.
    @Test
    void whenProvidersFail_andCacheIsPopulated_shouldReturnStaleData() {
        // Given
        WeatherResponse staleResponse = WeatherResponse.builder()
                .temperatureDegrees(20.0)
                .windSpeed(15.0)
                .build();
        
        // Populate the cache manually for the test
        Cache cache = cacheManager.getCache("weather-cache");
        if (cache != null) {
            // Spring Cache wraps reactive types, so we cache the Mono
            cache.put("Melbourne", Mono.just(staleResponse));
        }

        // Mock the service to simulate a total provider failure
        when(weatherService.getWeatherForCity()).thenReturn(Mono.error(new AllProvidersDownException("All down", new Throwable())));

        // When & Then
        // The controller should catch the exception and serve the stale data from the cache
        webTestClient.get().uri("/v1/weather/melbourne")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeatherResponse.class).isEqualTo(staleResponse);
    }

    @Test
    void whenProvidersFail_andCacheIsEmpty_shouldReturnServiceUnavailable() {
        // Given
        // Ensure cache is empty (done in @BeforeEach)
        
        // Mock the service to simulate a total provider failure
        when(weatherService.getWeatherForCity()).thenReturn(Mono.error(new AllProvidersDownException("All down", new Throwable())));

        // When & Then
        webTestClient.get().uri("/v1/weather/melbourne")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody(String.class).isEqualTo("Weather service is temporarily unavailable. Please try again later.");
    }
}
