# Zai Code Challenge - Melbourne Weather Service

This project is a solution to the Zai Code Challenge. It is an HTTP service built with **Java 17** and **Spring Boot 3** that reports the current weather in Melbourne.

The service is designed with scalability, reliability, and maintainability in mind, sourcing its data from two external weather providers with a primary/failover strategy.

## Table of Contents

1.  [Features](#features)
2.  [Architectural Design](#architectural-design)
3.  [API Endpoint](#api-endpoint)
4.  [Prerequisites](#prerequisites)
5.  [How to Run](#how-to-run)
    *   [Using Docker (Recommended)](#using-docker-recommended)
    *   [Running Locally with Maven](#running-locally-with-maven)
6.  [How to Test](#how-to-test)
7.  [Trade-offs and Further Improvements](#trade-offs-and-further-improvements)

## Features

-   **Unified Weather API**: Provides a simple, consistent JSON response for Melbourne's weather.
-   **Provider Failover**: Uses WeatherStack as the primary data source and automatically fails over to OpenWeatherMap if the primary provider is down.
-   **Caching**: Caches successful weather responses for 3 seconds to reduce latency and limit calls to external APIs.
-   **Stale Data on Failure**: If both providers are unavailable, the service will serve the last known good data from the cache, ensuring high availability for customers.
-   **Non-Blocking I/O**: Built with Spring WebFlux and `WebClient` for a fully asynchronous, non-blocking architecture that scales efficiently under load.
-   **Configuration Driven**: All external details (API keys, URLs, cache settings) are managed in `application.yml` and are not hard-coded.

## Architectural Design

The application follows modern microservice design principles with a clear separation of concerns.

-   **Controller Layer (`/controller`)**: Handles incoming HTTP requests, orchestrates calls to the service layer, and manages the "stale-on-error" cache retrieval logic.
-   **Service Layer (`/service`)**: Contains the core business logic, including the provider failover strategy and caching annotations (`@Cacheable`).
-   **Client Layer (`/client`)**: Implements an interface-driven design with a `WeatherProviderClient` interface. Each external provider (`WeatherStack`, `OpenWeatherMap`) has its own concrete implementation. This makes the system pluggable—adding a new provider only requires adding a new client implementation.
-   **Configuration (`/config`)**: Centralizes the creation of shared beans, such as the `WebClient.Builder`, to ensure consistent configurations (e.g., HTTP timeouts) across all clients.
-   **DTOs (`/dto`)**: Uses Data Transfer Objects to create a clear boundary between our internal API model (`WeatherResponse`) and the different models of external providers.

This design makes the codebase easy to understand, test, and extend, allowing new developers to make changes safely.

## API Endpoint

### Get Melbourne Weather

Returns the current temperature and wind speed in Melbourne.

-   **URL**: `/v1/weather/melbourne`
-   **Method**: `GET`
-   **Success Response (200 OK)**:
    ```json
    {
      "wind_speed": 15.0,
      "temperature_degrees": 21.0
    }
    ```
-   **Error Response (503 Service Unavailable)**:
    Returned only if both providers are down *and* there is no stale data in the cache.
    ```json
    "Weather service is temporarily unavailable. Please try again later."
    ```

## Prerequisites

-   **Java 17** or higher
-   **Apache Maven 3.8** or higher
-   **(Optional but Recommended)** **Docker** and **Docker Compose**
-   **API Keys**:
    1.  **WeatherStack**: Sign up for a free key at [weatherstack.com](https://weatherstack.com/)
    2.  **OpenWeatherMap**: Sign up for a free key at [openweathermap.org/api](https://openweathermap.org/api)

## How to Run

### Using Docker (Recommended)

This is the simplest way to run the application, as it doesn't require a local Java or Maven installation.

1.  **Create a `.env` file** in the project root with your API keys:
    ```env
    WEATHERSTACK_API_KEY=your_weatherstack_key_here
    OPENWEATHERMAP_APPID=your_openweathermap_key_here
    ```

2.  **Build and run the container** using Docker Compose:
    ```bash
    docker-compose up --build
    ```
    The service will be available at `http://localhost:8080`.

### Running Locally with Maven

1.  **Set Environment Variables** for your API keys:
    ```bash
    # For Linux/macOS
    export WEATHERSTACK_API_KEY="your_weatherstack_key_here"
    export OPENWEATHERMAP_APPID="your_openweathermap_key_here"

    # For Windows (Command Prompt)
    set WEATHERSTACK_API_KEY="your_weatherstack_key_here"
    set OPENWEATHERMAP_APPID="your_openweathermap_key_here"
    ```
    The application is configured in `application.yml` to read these variables.

2.  **Build the project** using Maven:
    ```bash
    mvn clean install
    ```

3.  **Run the application**:
    ```bash
    java -jar target/weather-challenge-0.0.1-SNAPSHOT.jar
    ```
    The service will be available at `http://localhost:8080`.

## How to Test

Once the application is running, you can test the endpoint using `curl` or any HTTP client:

```bash
curl -i http://localhost:8080/v1/weather/melbourne