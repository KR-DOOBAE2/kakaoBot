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
                                 @Value("${app.weather-url:https://api.met.no}") String weatherUrl) {
        this.rounds = rounds;
        this.locations = locations;
        this.weather = builder.baseUrl(weatherUrl)
                .defaultHeader("User-Agent", "Jeommechurial/0.1 (https://kakaobot-nkrp.onrender.com)")
                .build();
    }

    @GetMapping
    public Weather current(@PathVariable UUID id, @RequestParam String token) {
        RoundView round = rounds.get(id, token);
        if (round.officeAddress() == null || round.officeAddress().isBlank())
            throw new IllegalStateException("회사 주소를 설정하면 날씨를 볼 수 있어요.");
        String address = round.officeAddress();
        return cache.compute(address, (key, prior) -> prior != null && Instant.now().isBefore(prior.expiresAt())
                ? prior : new Cached(fetch(address), Instant.now().plus(Duration.ofHours(3)))).weather();
    }

    private Weather fetch(String address) {
        RestaurantSearch.Location point = locations.geocode(address);
        double latitude = Math.round(point.latitude() * 10_000d) / 10_000d;
        double longitude = Math.round(point.longitude() * 10_000d) / 10_000d;
        JsonNode root = weather.get().uri(builder -> builder.path("/weatherapi/locationforecast/2.0/compact")
                .queryParam("lat", latitude).queryParam("lon", longitude).build())
                .retrieve().body(JsonNode.class);
        JsonNode first = root == null ? null : root.path("properties").path("timeseries").path(0);
        JsonNode details = first == null ? null : first.path("data").path("instant").path("details");
        if (first == null || details == null || !details.has("air_temperature"))
            throw new IllegalStateException("현재 날씨를 불러오지 못했어요.");
        JsonNode data = first.path("data");
        String symbol = data.path("next_1_hours").path("summary").path("symbol_code").asText();
        if (symbol.isBlank()) symbol = data.path("next_6_hours").path("summary").path("symbol_code").asText();
        if (symbol.isBlank()) symbol = data.path("next_12_hours").path("summary").path("symbol_code").asText();
        String theme = classifySymbol(symbol);
        return new Weather(theme, switch (theme) {
            case "sunny" -> "맑음";
            case "rain" -> "비";
            case "snow" -> "눈";
            default -> "흐림";
        }, details.path("air_temperature").asDouble(), first.path("time").asText(), "MET Norway");
    }

    static String classifySymbol(String symbol) {
        if (symbol.contains("snow") || symbol.contains("sleet")) return "snow";
        if (symbol.contains("rain")) return "rain";
        if (symbol.startsWith("clearsky") || symbol.startsWith("fair")) return "sunny";
        return "cloudy";
    }

    private record Cached(Weather weather, Instant expiresAt) {}
    public record Weather(String theme, String label, double temperatureCelsius, String observedAt, String source) {}
}

