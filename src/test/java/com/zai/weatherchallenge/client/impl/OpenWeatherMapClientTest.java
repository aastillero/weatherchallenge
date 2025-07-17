package com.zai.weatherchallenge.client.impl;

import com.zai.weatherchallenge.config.OpenWeatherMapProperties;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.dto.external.OpenWeatherMapResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenWeatherMapClientTest {

    @Test
    void toUnifiedResponse_shouldMapAndConvertCorrectly() {
        // Given
        OpenWeatherMapResponse.Main main = new OpenWeatherMapResponse.Main();
        main.setTemp(293.15); // 20 degrees Celsius

        OpenWeatherMapResponse.Wind wind = new OpenWeatherMapResponse.Wind();
        wind.setSpeed(4.16667); // 15 km/h

        OpenWeatherMapResponse openWeatherMapResponse = new OpenWeatherMapResponse();
        openWeatherMapResponse.setMain(main);
        openWeatherMapResponse.setWind(wind);

        OpenWeatherMapProperties properties = new OpenWeatherMapProperties("test-key", "test-url");
        WebClient.Builder webClientBuilder = WebClient.builder();
        OpenWeatherMapClient openWeatherMapClient = new OpenWeatherMapClient(webClientBuilder, properties);

        // When
        WeatherResponse weatherResponse = openWeatherMapClient.toUnifiedResponse(openWeatherMapResponse);

        // Then
        assertEquals(20.0, weatherResponse.temperatureDegrees());
        assertEquals(15.0, weatherResponse.windSpeed(), 0.01);
    }
}
