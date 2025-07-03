package com.zai.weatherchallenge.client.impl;

import com.zai.weatherchallenge.client.WeatherProviderClient;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.dto.external.OpenWeatherMapResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Qualifier("failover")
public class OpenWeatherMapClient implements WeatherProviderClient {

    private final WebClient webClient;
    private final String apiKey;

    public OpenWeatherMapClient(WebClient.Builder webClientBuilder, @Value("${app.providers.openweathermap.url}") String baseUrl, @Value("${app.providers.openweathermap.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public Mono<WeatherResponse> fetchWeather(String city) {
        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("q", city + ",AU")
                .queryParam("appid", apiKey)
                .build())
            .retrieve()
            .bodyToMono(OpenWeatherMapResponse.class)
            .map(this::toUnifiedResponse);
    }
    
    @Override
    public String getProviderName() {
        return "OpenWeatherMap";
    }

    private WeatherResponse toUnifiedResponse(OpenWeatherMapResponse response) {
        return WeatherResponse.builder()
            .temperatureDegrees(convertKelvinToCelsius(response.getMain().getTemp()))
            .windSpeed(convertMsToKph(response.getWind().getSpeed()))
            .build();
    }

    // OpenWeatherMap returns Kelvin, we need Celsius
    private double convertKelvinToCelsius(double kelvin) {
        return BigDecimal.valueOf(kelvin - 273.15)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // OpenWeatherMap returns m/s, let's unify to km/h like WeatherStack
    private double convertMsToKph(double ms) {
        return BigDecimal.valueOf(ms * 3.6)
                .setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}