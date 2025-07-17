# Senior Java Developer Interview Questions (Based on Weather Challenge Project)

This document contains a list of potential technical questions an interviewer might ask about this project, targeting a Senior Java Developer role. The questions are designed to probe understanding of the architectural choices, design patterns, and underlying technologies.

## Table of Contents

1.  [High-Level Architecture & Design](#high-level-architecture--design)
2.  [Spring Framework (Boot, WebFlux, Core)](#spring-framework-boot-webflux-core)
3.  [Resilience and Error Handling (Failover & Caching)](#resilience-and-error-handling-failover--caching)
4.  [Concurrency and Asynchronous Programming](#concurrency-and-asynchronous-programming)
5.  [API Design & DTOs](#api-design--dtos)
6.  [Configuration & Security](#configuration--security)
7.  [Testing & Maintainability](#testing--maintainability)
8.  [DevOps & Deployment (Docker)](#devops--deployment-docker)

---

### High-Level Architecture & Design

1.  **Question:** Can you walk me through the high-level architecture of this service? Why was a layered approach (Controller/Service/Client) chosen?
    *   **Answer:**
        *   **Architecture:** The service follows a standard 3-tier architecture:
            *   **Controller (`WeatherController`):** This is the entry point for all HTTP requests. It's responsible for handling the incoming request, validating it, calling the `WeatherService` to get the weather data, and then formatting the response (either success or error).
            *   **Service (`WeatherService`):** This layer contains the core business logic. It orchestrates calls to the different weather providers (primary and failover). It's also responsible for caching the results to improve performance and reduce API calls.
            *   **Client (`WeatherProviderClient` implementations):** This layer is responsible for the communication with the external weather APIs (WeatherStack and OpenWeatherMap). Each provider has its own implementation of the `WeatherProviderClient` interface. This isolates the core application from the specifics of each external API.
        *   **Why layered approach:**
            *   **Separation of Concerns:** Each layer has a distinct responsibility. This makes the code easier to understand, develop, and maintain.
            *   **Testability:** Each layer can be tested independently. For example, we can test the business logic in the service layer by mocking the client layer.
            *   **Maintainability:** Changes in one layer have minimal impact on other layers. For example, if we want to change the caching strategy, we only need to modify the service layer. If a provider changes its API, we only need to update the corresponding client implementation.

2.  **Question:** The README mentions the system is "pluggable," making it easy to add new weather providers. How is this achieved from a design perspective? What specific design pattern facilitates this?
    *   **Answer:**
        *   **How it's achieved:** The "pluggable" nature is achieved by programming to an interface, not an implementation.
            *   There is a `WeatherProviderClient` interface that defines a contract for what a weather provider client must do (i.e., a `getWeather` method).
            *   `WeatherStackClient` and `OpenWeatherMapClient` are concrete implementations of this interface.
            *   The `WeatherService` is not tightly coupled to any specific client. It depends on the `WeatherProviderClient` interface.
        *   **Design Pattern:** This is a classic example of the **Strategy Pattern**.
            *   The `WeatherProviderClient` interface is the `Strategy`.
            *   The concrete implementations (`WeatherStackClient`, `OpenWeatherMapClient`) are the `Concrete Strategies`.
            *   The `WeatherService` is the `Context` that uses a strategy.
            *   Spring's Dependency Injection is used to inject the specific strategies (clients) into the service. The `@Qualifier` annotation is used to specify which implementation to inject as the primary and which as the failover.

3.  **Question:** The project uses different Data Transfer Objects (DTOs) for the internal API response (`WeatherResponse`) and for mapping responses from external providers. What are the benefits and potential drawbacks of this approach?
    *   **Answer:**
        *   **Benefits (Anti-Corruption Layer):**
            *   **Decoupling:** The primary benefit is creating a boundary between our internal domain model and the data models of the external services. This is an example of an **Anti-Corruption Layer**.
            *   **Stability:** Our internal `WeatherResponse` is stable and controlled by us. If an external provider changes their API response, we only need to update the corresponding external DTO and the mapping logic in the client.
            *   **Simplicity:** Our internal `WeatherResponse` can be much simpler and tailored to what our clients need.
        *   **Drawbacks:**
            *   **Boilerplate Code:** It requires writing extra DTO classes and the mapping logic to convert from the external DTO to the internal one.

### Spring Framework (Boot, WebFlux, Core)

4.  **Question:** This project is built on Spring WebFlux instead of the traditional Spring Web MVC. What are the key differences, and why is WebFlux a suitable choice for an I/O-bound service like this?
    *   **Answer:**
        *   **Key Differences:**
            *   **Programming Model:** Web MVC uses a thread-per-request model (blocking). WebFlux uses an event-driven, non-blocking model built on Project Reactor.
            *   **Concurrency:** Web MVC handles concurrency with a large thread pool. WebFlux uses a small number of threads (event loops) to handle many concurrent requests.
            *   **API:** Web MVC uses Servlet API (e.g., `HttpServletRequest`). WebFlux uses reactive streams (`Mono`, `Flux`).
        *   **Why WebFlux is suitable:** This service is I/O-bound because it spends most of its time waiting for network calls to the external weather APIs. In a blocking model, the request thread would be idle waiting for the response. In a non-blocking model, the thread is released to handle other requests while waiting for the I/O to complete. This leads to better resource utilization and higher scalability.

5.  **Question:** How does Spring's dependency injection work to wire the components together (e.g., injecting the `WeatherStackClient` and `OpenWeatherMapClient` into the `WeatherService`)? Could you explain the roles of `@Component`, `@Service`, and `@Qualifier`?
    *   **Answer:**
        *   **Dependency Injection:** Spring's IoC (Inversion of Control) container creates objects (beans) and injects them into other objects that need them.
        *   **@Component:** A generic stereotype annotation that marks a class as a Spring-managed component. The container will create a bean for this class.
        *   **@Service:** A specialization of `@Component`. It indicates that the class holds business logic. It's functionally the same as `@Component` but used for semantic purposes.
        *   **@Qualifier:** When there are multiple beans of the same type (like our `WeatherProviderClient`), `@Qualifier` is used to specify which bean to inject. In this project, we'd use `@Qualifier("weatherStackClient")` and `@Qualifier("openWeatherMapClient")` to inject the correct clients into the `WeatherService`.

6.  **Question:** The `WebClient.Builder` is defined as a central `@Bean`. Why is this a good practice compared to each client creating its own `WebClient` instance?
    *   **Answer:**
        *   **Consistency:** It ensures that all `WebClient` instances share the same base configuration (e.g., connection timeouts, read/write timeouts, default headers).
        *   **Maintainability (DRY):** It follows the "Don't Repeat Yourself" principle. If we need to change a timeout, we only need to do it in one place.
        *   **Resource Sharing:** It allows for more efficient resource sharing, such as connection pools and thread pools.

### Resilience and Error Handling (Failover & Caching)

7.  **Question:** Describe the provider failover logic. What happens step-by-step when a request comes in and the primary provider (WeatherStack) fails to respond?
    *   **Answer:**
        1.  The `WeatherService` calls the `getWeather` method on the primary client (`WeatherStackClient`).
        2.  This returns a `Mono<WeatherResponse>`.
        3.  We use the `onErrorResume` operator on this `Mono`.
        4.  If the primary client's `Mono` completes successfully, the `onErrorResume` block is skipped.
        5.  If the primary client's `Mono` signals an error (e.g., a timeout or a 5xx response), the `onErrorResume` block is executed.
        6.  Inside this block, we call the `getWeather` method on the failover client (`OpenWeatherMapClient`).
        7.  The `Mono` returned by the failover client is then returned as the result of the chain.

8.  **Question:** The service uses Spring's `@Cacheable` for caching. How does this annotation work under the hood? What are the limitations of this simple caching approach, and when might you need a more robust solution like an external Redis cache?
    *   **Answer:**
        *   **How it works:** `@Cacheable` is an AOP (Aspect-Oriented Programming) annotation. Spring creates a proxy around the `WeatherService` bean. When the `getWeather` method is called, the proxy intercepts the call. It checks the cache (`weather-cache`) for an entry with a key corresponding to the method arguments. If found, it returns the cached value. If not, it calls the actual method, and before returning, it stores the result in the cache.
        *   **Limitations:**
            *   **In-memory:** The default cache is in-memory (Caffeine in this project). This means the cache is local to each instance of the service. In a distributed environment with multiple instances, caches will be inconsistent.
            *   **Eviction Policies:** It relies on simple eviction policies like TTL (Time-to-Live).
        *   **When to use Redis:** You'd use a distributed cache like Redis when:
            *   You are running multiple instances of the service and need a consistent cache.
            *   You need more advanced data structures and eviction policies.
            *   You need the cache to survive application restarts.

9.  **Question:** A key feature is serving stale data if both providers are down. Discuss the trade-offs of this decision. In what business scenarios would this be a good feature, and when would it be unacceptable?
    *   **Answer:**
        *   **Trade-offs (Availability vs. Consistency):** This is a classic trade-off. We are prioritizing **Availability** (the service is always up and returns *something*) over **Consistency** (the data is not guaranteed to be the most up-to-date).
        *   **Good Scenarios:** For non-critical use cases like a general weather display on a website, slightly stale data is perfectly acceptable. The user would rather see yesterday's weather than an error message.
        *   **Unacceptable Scenarios:** This would be unacceptable for use cases where real-time accuracy is critical, such as for aviation, maritime navigation, or scientific research.

10. **Question:** How would you enhance the resilience of this service further? What patterns or tools would you consider?
    *   **Answer:**
        *   **Circuit Breaker:** Implement a Circuit Breaker pattern (e.g., using Resilience4j). If the primary provider fails repeatedly, the circuit breaker would "open," and we would fail-fast to the secondary provider without waiting for a timeout. This prevents cascading failures.
        *   **Retries with Exponential Backoff:** For transient network errors, we could implement a retry mechanism on the client calls.
        *   **Bulkhead:** Isolate calls to different providers in separate thread pools to prevent a slow provider from exhausting all available threads.

### Concurrency and Asynchronous Programming

11. **Question:** Can you explain what "non-blocking I/O" means in the context of this application? How does it help the service scale and handle more concurrent requests compared to a traditional blocking model?
    *   **Answer:**
        *   **Non-blocking I/O:** When the application makes a network call (I/O), it doesn't block the execution thread waiting for the response. Instead, it registers a callback and releases the thread. When the response is available, the event loop notifies the application, and the callback is executed on another thread.
        *   **How it helps scaling:** In a traditional blocking model, each concurrent request holds a thread. If you have 1000 concurrent requests, you need 1000 threads, which is very resource-intensive. With non-blocking I/O, a small number of threads (an event loop) can handle a large number of concurrent requests because the threads are not blocked waiting. This leads to much better resource utilization and allows the service to handle a higher load.

12. **Question:** What is a `Mono` in Project Reactor? Can you trace the execution flow of the `Mono<WeatherResponse>` from the controller down to the `WebClient` call and back?
    *   **Answer:**
        *   **Mono:** A `Mono` is a reactive stream publisher from Project Reactor that can emit at most one item (`onNext`), or a completion signal (`onComplete`), or an error signal (`onError`).
        *   **Execution Flow:**
            1.  A request hits the `WeatherController`.
            2.  The controller calls the `WeatherService.getWeather()` method, which returns a `Mono<WeatherResponse>`. At this point, nothing has happened yet (it's just a declaration of a pipeline).
            3.  The controller returns this `Mono` to the WebFlux framework.
            4.  WebFlux subscribes to the `Mono`. This is the trigger that starts the execution.
            5.  The `Mono` from the `WeatherService` starts executing. It calls the primary provider's `getWeather` method.
            6.  The `WebClient` makes the HTTP call asynchronously.
            7.  When the response comes back, the `WebClient`'s `Mono` emits the response body.
            8.  This is mapped to our internal `WeatherResponse` DTO.
            9.  The `Mono` from the service emits this `WeatherResponse`.
            10. WebFlux receives the item and sends it as the HTTP response.

### API Design & DTOs

13. **Question:** The API is versioned (`/v1`). Why is this a crucial practice for public-facing APIs?
    *   **Answer:** API versioning is crucial for managing change. It allows you to introduce breaking changes in a new version (`/v2`) without affecting existing clients who are still using the old version (`/v1`). This ensures backward compatibility and provides a smooth migration path for consumers of your API.

14. **Question:** If you were to add a new field to the `WeatherResponse` DTO, what other parts of the application would you need to consider or modify?
    *   **Answer:**
        1.  **`WeatherResponse` DTO:** Add the new field to this class.
        2.  **Client Implementations (`WeatherStackClient`, `OpenWeatherMapClient`):** Update the mapping logic in these classes to extract the new field from the external provider's response DTO and populate it in our `WeatherResponse`.
        3.  **External DTOs (Optional):** If the new field is not already present in the external DTOs, you would need to add it there first.
        4.  **Tests:** Update unit and integration tests to assert the value of the new field.

### Configuration & Security

15. **Question:** The application uses environment variables for API keys. Why is this a more secure practice than storing them directly in `application.yml` and committing the file to source control?
    *   **Answer:** Storing secrets in source control is a major security risk. If the repository is compromised or made public, the secrets are exposed. Environment variables decouple the secrets from the codebase. The code is the same in all environments, but the secrets are injected at runtime from the environment, which is a much more secure and flexible approach. It aligns with the Twelve-Factor App methodology.

16. **Question:** How would you manage different configurations for `dev`, `staging`, and `production` environments in a Spring Boot application?
    *   **Answer:** Spring Boot has built-in support for this using **Profiles**. You can create separate configuration files for each environment, such as `application-dev.yml`, `application-staging.yml`, and `application-prod.yml`. You can then activate a specific profile by setting the `spring.profiles.active` property (e.g., via an environment variable).

### Testing & Maintainability

17. **Question:** How would you write a unit test for the `WeatherService` to specifically verify the failover logic? What dependencies would you need to mock, and how would you do it?
    *   **Answer:**
        *   **Dependencies to mock:** You would need to mock both `WeatherProviderClient` beans (`weatherStackClient` and `openWeatherMapClient`).
        *   **How to do it (using Mockito and StepVerifier):**
            1.  Use `@Mock` to create mocks for the two clients.
            2.  Use `when(weatherStackClient.getWeather(any())).thenReturn(Mono.error(new RuntimeException("Primary provider down")));` to simulate the primary provider failing.
            3.  Use `when(openWeatherMapClient.getWeather(any())).thenReturn(Mono.just(new WeatherResponse(...)));` to have the failover provider return a successful response.
            4.  Call the `weatherService.getWeather()` method.
            5.  Use `StepVerifier` to subscribe to the resulting `Mono` and assert that it emits the expected `WeatherResponse` from the mocked failover client and then completes.

18. **Question:** What is the difference between the unit tests you would write for the service layer and the integration tests you might write for the controller layer?
    *   **Answer:**
        *   **Unit Tests (Service Layer):** These tests focus on a single class (`WeatherService`) in isolation. All external dependencies (like the clients) are mocked. The goal is to verify the business logic of the service (e.g., failover, caching) quickly and reliably.
        *   **Integration Tests (Controller Layer):** These tests verify the interaction between multiple components. For the controller, you would typically use `@SpringBootTest` with `WebTestClient`. This starts up a mock server environment and allows you to make real HTTP requests to your controller endpoint. You might still mock the external clients, but you are testing the full flow from the controller, through serialization/deserialization, to the service.

### DevOps & Deployment (Docker)

19. **Question:** What are the roles of the `Dockerfile` and the `docker-compose.yml` file in this project? Why are both used?
    *   **Answer:**
        *   **`Dockerfile`:** This is a blueprint for building a Docker **image**. It contains instructions on how to build the application's runtime environment, including the base OS, Java version, application JAR, and how to run it.
        *   **`docker-compose.yml`:** This is a tool for defining and running multi-container Docker **applications**. In this project, it's used to define the `weather-service` **service**, build the image using the `Dockerfile`, and configure the running **container** (e.g., map ports, inject environment variables from a `.env` file).
        *   **Why both:** They serve different purposes. The `Dockerfile` builds the artifact (the image). `docker-compose` uses that artifact to run and manage the application container in a declarative way.

20. **Question:** What are the primary benefits of running this application inside a Docker container as recommended, compared to running it directly on a host machine using `mvn spring-boot:run`?
    *   **Answer:**
        *   **Consistency:** A container packages the application and all its dependencies (OS, Java version, etc.) into a single, immutable unit. This ensures that the application runs exactly the same way in every environment (dev, staging, prod).
        *   **Portability:** The container can run on any machine that has Docker installed, regardless of the underlying OS.
        *   **Isolation:** The container runs in an isolated environment, so it doesn't interfere with other applications on the host machine.
        *   **Scalability:** Containers are lightweight and can be easily scaled up or down using container orchestration platforms like Kubernetes.
