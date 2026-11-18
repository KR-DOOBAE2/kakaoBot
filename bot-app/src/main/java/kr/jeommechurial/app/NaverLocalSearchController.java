package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/** Displays NAVER API HUB local search results in their original order, without persisting them. */
@RestController
public class NaverLocalSearchController {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String keyId;
    private final String key;

    public NaverLocalSearchController(RestClient.Builder builder, ObjectMapper mapper,
            @Value("${app.naver-api-key-id:}") String keyId,
            @Value("${app.naver-api-key:}") String key) {
        this.client = builder.baseUrl("https://naverapihub.apigw.ntruss.com").build();
        this.mapper = mapper;
        this.keyId = keyId;
        this.key = key;
    }

    @PostMapping("/api/naver/local-search")
    public Result search(@RequestBody Request request) {
        if (request == null || request.address() == null || request.address().isBlank()
                || request.address().length() > 160 || request.menu() == null
                || request.menu().isBlank() || request.menu().length() > 80) {
            throw new IllegalArgumentException("회사 주소와 메뉴를 확인해주세요.");
        }
        if (keyId.isBlank() || key.isBlank()) return new Result(false, List.of());
        String area = request.address().replaceAll("\\b\\d+(?:-\\d+)?\\b", "").replaceAll("\\s+", " ").strip();
        String query = area + " " + request.menu().strip();
        byte[] body = client.get().uri(uri -> uri.path("/search/v1/local")
                        .queryParam("query", query).queryParam("display", 5)
                        .queryParam("start", 1).queryParam("sort", "random")
                        .queryParam("format", "json").build())
                .header("X-NCP-APIGW-API-KEY-ID", keyId)
                .header("X-NCP-APIGW-API-KEY", key)
                .retrieve().body(byte[].class);
        JsonNode response;
        try {
            response = mapper.readTree(new String(body, StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("네이버 검색 응답을 읽지 못했어요.", e);
        }
        List<Place> places = new ArrayList<>();
        if (response != null && response.path("items").isArray()) {
            for (JsonNode item : response.path("items")) {
                places.add(new Place(item.path("title").asText(""),
                        item.path("category").asText(""),
                        item.path("address").asText(""),
                        item.path("roadAddress").asText(""),
                        item.path("link").asText("")));
            }
        }
        return new Result(true, List.copyOf(places));
    }

    public record Request(String address, String menu) {}
    public record Place(String title, String category, String address, String roadAddress, String link) {}
    public record Result(boolean configured, List<Place> places) {}
}

