package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.JsonNode;
import kr.jeommechurial.core.RoundView;
import kr.jeommechurial.service.LunchService;
import kr.jeommechurial.service.RestaurantSearch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/rounds/{id}/weather")
public class TeamWeatherController {
    private final LunchService rounds;
    private final RestaurantSearch locations;
    private final RestClient weather;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public TeamWeatherController(LunchService rounds, RestaurantSearch locations, RestClient.Builder builder,
                                 @Value("${app.weather-url:https://api.open-meteo.com}") String weatherUrl) {
        this.rounds = rounds;
        this.locations = locations;
        this.weather = builder.baseUrl(weatherUrl).build();
    }

    @GetMapping
    public Weather current(@PathVariable UUID id, @RequestParam String token) {
        RoundView round = rounds.get(id, token);
        if (round.officeAddress() == null || round.officeAddress().isBlank())
            throw new IllegalStateException("회사 주소를 설정하면 날씨를 볼 수 있어요.");
        String address = round.officeAddress();
        return cache.compute(address, (key, prior) -> prior != null && Instant.now().isBefore(prior.expiresAt())
                ? prior : new Cached(fetch(address), Instant.now().plus(Duration.ofMinutes(30)))).weather();
    }

    private Weather fetch(String address) {
        RestaurantSearch.Location point = locations.geocode(address);
        JsonNode root = weather.get().uri(builder -> builder.path("/v1/forecast")
                .queryParam("latitude", point.latitude()).queryParam("longitude", point.longitude())
                .queryParam("current", "temperature_2m,weather_code,is_day")
                .queryParam("timezone", "Asia/Seoul").build()).retrieve().body(JsonNode.class);
        JsonNode current = root == null ? null : root.path("current");
        if (current == null || !current.has("weather_code") || !current.has("temperature_2m"))
            throw new IllegalStateException("현재 날씨를 불러오지 못했어요.");
        String theme = classify(current.path("weather_code").asInt());
        return new Weather(theme, switch (theme) {
            case "sunny" -> "맑음";
            case "rain" -> "비";
            case "snow" -> "눈";
            default -> "흐림";
        }, current.path("temperature_2m").asDouble(), current.path("time").asText(), "Open-Meteo");
    }

    static String classify(int code) {
        if (code == 0 || code == 1) return "sunny";
        if (code == 2 || code == 3 || code == 45 || code == 48) return "cloudy";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "snow";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82) || code >= 95) return "rain";
        return "cloudy";
    }

    private record Cached(Weather weather, Instant expiresAt) {}
    public record Weather(String theme, String label, double temperatureCelsius, String observedAt, String source) {}
}

