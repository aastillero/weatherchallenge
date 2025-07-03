package com.zai.weatherchallenge.client.impl;

import com.zai.weatherchallenge.client.WeatherProviderClient;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.dto.external.WeatherStackResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Qualifier("primary")
public class WeatherStackClient implements WeatherProviderClient {

    private final WebClient webClient;
    private final String apiKey;

    public WeatherStackClient(WebClient.Builder webClientBuilder, @Value("${app.providers.weatherstack.url}") String baseUrl, @Value("${app.providers.weatherstack.api-key}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public Mono<WeatherResponse> fetchWeather(String city) {
        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("access_key", apiKey)
                .queryParam("query", city)
                .build())
            .retrieve()
            .bodyToMono(WeatherStackResponse.class)
            .map(this::toUnifiedResponse);
    }
    
    @Override
    public String getProviderName() {
        return "WeatherStack";
    }

    private WeatherResponse toUnifiedResponse(WeatherStackResponse response) {
        return WeatherResponse.builder()
            .temperatureDegrees(response.getCurrent().getTemperature())
            .windSpeed(response.getCurrent().getWind_speed())
            .build();
    }
}
