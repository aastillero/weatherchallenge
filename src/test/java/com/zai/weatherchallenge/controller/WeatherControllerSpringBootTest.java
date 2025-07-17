package com.zai.weatherchallenge.controller;

import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.service.WeatherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WeatherControllerSpringBootTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    void whenGetWeather_andServiceSucceeds_shouldReturnOk() {
        // Given
        WeatherResponse weatherResponse = WeatherResponse.builder()
                .temperatureDegrees(20.0)
                .windSpeed(15.0)
                .build();

        when(weatherService.getWeatherForCity()).thenReturn(Mono.just(weatherResponse));

        // When & Then
        webTestClient.get().uri("/v1/weather/melbourne")
                .exchange()
                .expectStatus().isOk()
                .expectBody(WeatherResponse.class).isEqualTo(weatherResponse);
    }
}