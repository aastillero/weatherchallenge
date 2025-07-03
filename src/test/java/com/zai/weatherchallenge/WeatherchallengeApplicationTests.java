package com.zai.weatherchallenge;

import com.zai.weatherchallenge.client.impl.OpenWeatherMapClient;
import com.zai.weatherchallenge.client.impl.WeatherStackClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class WeatherchallengeApplicationTests {

	@MockBean
	private OpenWeatherMapClient openWeatherMapClient;

	@MockBean
	private WeatherStackClient weatherStackClient;

	@Test
	void contextLoads() {
	}

}