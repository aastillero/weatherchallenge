package com.zai.weatherchallenge.client.impl;

import com.zai.weatherchallenge.config.WeatherStackProperties;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.dto.external.WeatherStackResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherStackClientTest {

    @Test
    void toUnifiedResponse_shouldMapCorrectly() {
        // Given
        WeatherStackResponse.Current current = new WeatherStackResponse.Current();
        current.setTemperature(20.0);
        current.setWind_speed(15.0);

        WeatherStackResponse weatherStackResponse = new WeatherStackResponse();
        weatherStackResponse.setCurrent(current);

        WeatherStackProperties properties = new WeatherStackProperties("test-key", "test-url");
        WebClient.Builder webClientBuilder = WebClient.builder();
        WeatherStackClient weatherStackClient = new WeatherStackClient(webClientBuilder, properties);

        // When
        WeatherResponse weatherResponse = weatherStackClient.toUnifiedResponse(weatherStackResponse);

        // Then
        assertEquals(20.0, weatherResponse.temperatureDegrees());
        assertEquals(15.0, weatherResponse.windSpeed());
    }
}
