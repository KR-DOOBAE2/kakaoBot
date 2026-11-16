package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.JsonNode;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.service.RestaurantSearch;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "app.place-provider", havingValue = "osm", matchIfMissing = true)
public class OpenStreetMapRestaurantAdapter implements RestaurantSearch {
    private static final String USER_AGENT = "Jeommechurial/0.1 (https://kakaobot-nkrp.onrender.com)";
    private static final long EARTH_RADIUS_METERS = 6_371_000;
    private final RestClient geocoder;
    private final RestClient places;
    private final Map<String, Location> geocodeCache = new ConcurrentHashMap<>();
    private long lastGeocodeAt;

    public OpenStreetMapRestaurantAdapter(RestClient.Builder builder,
                                          @Value("${app.osm-geocoder-url:https://nominatim.openstreetmap.org}") String geocoderUrl,
                                          @Value("${app.osm-overpass-url:https://overpass-api.de}") String overpassUrl) {
        this.geocoder = builder.baseUrl(geocoderUrl).defaultHeader("User-Agent", USER_AGENT).build();
        this.places = builder.baseUrl(overpassUrl).defaultHeader("User-Agent", USER_AGENT).build();
    }

    @Override
    public synchronized Location geocode(String address) {
        String query = address.strip();
        Location cached = geocodeCache.get(query);
        if (cached != null) return cached;
        long remaining = 1_000_000_000L - (System.nanoTime() - lastGeocodeAt);
        if (lastGeocodeAt != 0 && remaining > 0) {
            try {
                Thread.sleep(remaining / 1_000_000L + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("주소 검색이 중단됐어요.", e);
            }
        }
        lastGeocodeAt = System.nanoTime();
        JsonNode results = geocoder.get().uri(b -> b.path("/search").queryParam("q", query)
                        .queryParam("format", "jsonv2").queryParam("limit", 1)
                        .queryParam("countrycodes", "kr").build())
                .header("Accept-Language", "ko").retrieve().body(JsonNode.class);
        if (results == null || !results.isArray() || results.isEmpty())
            throw new IllegalArgumentException("주소를 찾지 못했어요. 도로명 주소나 건물 이름을 확인해주세요.");
        Location location = new Location(results.get(0).path("lat").asDouble(),
                results.get(0).path("lon").asDouble());
        geocodeCache.put(query, location);
        return location;
    }

    @Override
    public List<PlaceSearchResult> nearbyRestaurants(double latitude, double longitude, int radiusMeters) {
        String query = "[out:json][timeout:20];nwr[\"amenity\"~\"^(restaurant|fast_food|food_court)$\"]"
                + "(around:" + radiusMeters + "," + latitude + "," + longitude + ");out center;";
        JsonNode response = places.get().uri(b -> b.path("/api/interpreter").queryParam("data", query).build())
                .retrieve().body(JsonNode.class);
        return parseRestaurants(response, latitude, longitude, radiusMeters);
    }

    static List<PlaceSearchResult> parseRestaurants(JsonNode response, double latitude, double longitude, int radiusMeters) {
        if (response == null || !response.path("elements").isArray()) return List.of();
        List<PlaceSearchResult> found = new ArrayList<>();
        for (JsonNode element : response.path("elements")) {
            JsonNode tags = element.path("tags");
            String name = tags.path("name:ko").asText(tags.path("name").asText("")).strip();
            if (name.isBlank()) continue;
            double lat = element.has("lat") ? element.path("lat").asDouble() : element.path("center").path("lat").asDouble();
            double lon = element.has("lon") ? element.path("lon").asDouble() : element.path("center").path("lon").asDouble();
            int distance = distanceMeters(latitude, longitude, lat, lon);
            if (distance > radiusMeters) continue;
            String type = element.path("type").asText();
            String id = element.path("id").asText();
            String address = address(tags);
            found.add(new PlaceSearchResult("osm-" + type + "-" + id, name, address,
                    tags.path("cuisine").asText(tags.path("amenity").asText()),
                    tags.path("phone").asText(""), "https://www.openstreetmap.org/" + type + "/" + id,
                    lat, lon, distance));
        }
        return found.stream().sorted(Comparator.comparingInt(PlaceSearchResult::distanceMeters)).toList();
    }

    private static String address(JsonNode tags) {
        String street = tags.path("addr:street").asText("").strip();
        String number = tags.path("addr:housenumber").asText("").strip();
        String city = tags.path("addr:city").asText("").strip();
        String combined = (city + " " + street + " " + number).strip();
        return combined.isBlank() ? "주소 정보 없음 · 지도 링크에서 위치 확인" : combined;
    }

    static int distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi = Math.toRadians(lat2 - lat1);
        double lambda = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(phi / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(lambda / 2), 2);
        return (int) Math.round(2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(a)));
    }
}
