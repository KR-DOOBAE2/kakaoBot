package kr.jeommechurial.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamWeatherControllerTest {
    @Test void groupsForecastSymbolsIntoFourThemes() {
        assertEquals("sunny", TeamWeatherController.classifySymbol("clearsky_day"));
        assertEquals("sunny", TeamWeatherController.classifySymbol("fair_night"));
        assertEquals("cloudy", TeamWeatherController.classifySymbol("partlycloudy_day"));
        assertEquals("rain", TeamWeatherController.classifySymbol("lightrainshowers_day"));
        assertEquals("snow", TeamWeatherController.classifySymbol("lightsnowshowers_day"));
        assertEquals("snow", TeamWeatherController.classifySymbol("sleet"));
    }
}

