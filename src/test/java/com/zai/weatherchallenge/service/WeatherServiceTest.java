package com.zai.weatherchallenge.service;

import com.zai.weatherchallenge.client.WeatherProviderClient;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.exception.AllProvidersDownException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock(name = "primaryWeatherProvider")
    private WeatherProviderClient primaryProvider;

    @Mock(name = "failoverWeatherProvider")
    private WeatherProviderClient failoverProvider;

    @InjectMocks
    private WeatherService weatherService;

    @Test
    void whenBothProvidersFail_shouldThrowAllProvidersDownException() {
        // Given
        String city = "Melbourne";
        weatherService = new WeatherService(primaryProvider, failoverProvider, city, "weather-cache");

        when(primaryProvider.fetchWeather(city)).thenReturn(Mono.error(new RuntimeException("Primary provider failed")));
        when(failoverProvider.fetchWeather(city)).thenReturn(Mono.error(new RuntimeException("Failover provider failed")));

        // When
        Mono<WeatherResponse> result = weatherService.getWeatherForCity();

        // Then
        StepVerifier.create(result)
                .expectError(AllProvidersDownException.class)
                .verify();

        verify(primaryProvider).fetchWeather(city);
        verify(failoverProvider).fetchWeather(city);
    }

    @Test
    void whenPrimaryProviderSucceeds_shouldNotCallFailoverProvider() {
        // Given
        String city = "Melbourne";
        weatherService = new WeatherService(primaryProvider, failoverProvider, city, "weather-cache");

        WeatherResponse primaryResponse = WeatherResponse.builder()
                .temperatureDegrees(20.0)
                .windSpeed(5.0)
                .build();

        when(primaryProvider.fetchWeather(city)).thenReturn(Mono.just(primaryResponse));

        // When
        Mono<WeatherResponse> result = weatherService.getWeatherForCity();

        // Then
        StepVerifier.create(result)
                .expectNext(primaryResponse)
                .verifyComplete();

        verify(primaryProvider).fetchWeather(city);
        verify(failoverProvider, never()).fetchWeather(city);
    }

    @Test
    void whenPrimaryProviderFails_shouldCallFailoverProvider() {
        // Given
        String city = "Melbourne";
        weatherService = new WeatherService(primaryProvider, failoverProvider, city, "weather-cache");

        WeatherResponse failoverResponse = WeatherResponse.builder()
                .temperatureDegrees(25.0)
                .windSpeed(10.0)
                .build();
        when(primaryProvider.fetchWeather(city)).thenReturn(Mono.error(new RuntimeException("Primary provider failed")));
        when(failoverProvider.fetchWeather(city)).thenReturn(Mono.just(failoverResponse));

        when(primaryProvider.getProviderName()).thenReturn("Primary Provider");
        when(failoverProvider.getProviderName()).thenReturn("Failover Provider");

        // When
        Mono<WeatherResponse> result = weatherService.getWeatherForCity();

        // Then
        StepVerifier.create(result)
                .expectNext(failoverResponse)
                .verifyComplete();

        verify(primaryProvider).fetchWeather(city);
        verify(failoverProvider).fetchWeather(city);
        verify(primaryProvider).getProviderName();
        verify(failoverProvider).getProviderName();
    }
}
