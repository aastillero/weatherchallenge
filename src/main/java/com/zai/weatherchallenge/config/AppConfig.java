package com.zai.weatherchallenge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * General application configuration for creating shared beans.
 */
@Configuration
public class AppConfig {

    private static final int TIMEOUT_SECONDS = 5;

    /**
     * Creates a pre-configured WebClient.Builder bean that can be injected
     * into HTTP clients. This allows for centralized configuration of
     * common settings like timeouts.
     *
     * @return A configured WebClient.Builder instance.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        // Configure a client with a connection timeout. This is crucial for fault tolerance.
        // If a provider's server is unresponsive, we don't want our threads waiting indefinitely.
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}