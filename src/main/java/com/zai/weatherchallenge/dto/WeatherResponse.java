package com.zai.weatherchallenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record WeatherResponse(
    @JsonProperty("wind_speed")
    double windSpeed,

    @JsonProperty("temperature_degrees")
    double temperatureDegrees
) {}
