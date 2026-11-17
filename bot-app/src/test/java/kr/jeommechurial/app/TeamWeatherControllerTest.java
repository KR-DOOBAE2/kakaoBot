package kr.jeommechurial.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamWeatherControllerTest {
    @Test void groupsWmoWeatherCodesIntoFourThemes() {
        assertEquals("sunny", TeamWeatherController.classify(0));
        assertEquals("cloudy", TeamWeatherController.classify(3));
        assertEquals("rain", TeamWeatherController.classify(61));
        assertEquals("rain", TeamWeatherController.classify(95));
        assertEquals("snow", TeamWeatherController.classify(73));
        assertEquals("snow", TeamWeatherController.classify(85));
    }
}

