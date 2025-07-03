package com.zai.weatherchallenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.weather.providers.openweathermap")
public record OpenWeatherMapProperties(String apiKey, String url) {}
