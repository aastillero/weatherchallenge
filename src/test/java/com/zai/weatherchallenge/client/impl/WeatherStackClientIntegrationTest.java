package com.zai.weatherchallenge.client.impl;

import com.zai.weatherchallenge.config.WeatherStackProperties;
import com.zai.weatherchallenge.dto.WeatherResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeatherStackClientIntegrationTest {

    private MockWebServer mockWebServer;
    private WeatherStackClient weatherStackClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
        WeatherStackProperties properties = new WeatherStackProperties("test-key", baseUrl);
        weatherStackClient = new WeatherStackClient(WebClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void fetchWeather_shouldReturnCorrectResponse() {
        // Given
        String jsonResponse = "{\"current\":{\"temperature\":20,\"wind_speed\":15}}";
        mockWebServer.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

        // When
        Mono<WeatherResponse> result = weatherStackClient.fetchWeather("Melbourne");

        // Then
        StepVerifier.create(result)
                .assertNext(weatherResponse -> {
                    assertEquals(20.0, weatherResponse.temperatureDegrees());
                    assertEquals(15.0, weatherResponse.windSpeed());
                })
                .verifyComplete();
    }
}
