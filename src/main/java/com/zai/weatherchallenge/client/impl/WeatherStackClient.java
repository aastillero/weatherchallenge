package com.zai.weatherchallenge.client.impl;

import com.zai.weatherchallenge.client.WeatherProviderClient;
import com.zai.weatherchallenge.dto.WeatherResponse;
import com.zai.weatherchallenge.dto.external.WeatherStackResponse;
import com.zai.weatherchallenge.config.WeatherStackProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Qualifier("primary")
@EnableConfigurationProperties(WeatherStackProperties.class)
public class WeatherStackClient implements WeatherProviderClient {

    private final WebClient webClient;
    private final WeatherStackProperties properties;

    public WeatherStackClient(WebClient.Builder webClientBuilder, WeatherStackProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.url()).build();
        this.properties = properties;
    }

    @Override
    public Mono<WeatherResponse> fetchWeather(String city) {
        return this.webClient.get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("access_key", properties.apiKey())
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

    public WeatherResponse toUnifiedResponse(WeatherStackResponse response) {
        return WeatherResponse.builder()
            .temperatureDegrees(response.getCurrent().getTemperature())
            .windSpeed(response.getCurrent().getWind_speed())
            .build();
    }
}
