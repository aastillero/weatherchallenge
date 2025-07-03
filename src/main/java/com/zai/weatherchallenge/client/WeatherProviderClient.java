package com.zai.weatherchallenge.client;

import com.zai.weatherchallenge.dto.WeatherResponse;
import reactor.core.publisher.Mono;

public interface WeatherProviderClient {
    Mono<WeatherResponse> fetchWeather(String city);
    String getProviderName();
}