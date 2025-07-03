package com.zai.weatherchallenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.weather.providers.weatherstack")
public record WeatherStackProperties(String apiKey, String url) {}
