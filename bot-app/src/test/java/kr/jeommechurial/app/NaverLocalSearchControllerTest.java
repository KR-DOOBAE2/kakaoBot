package kr.jeommechurial.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NaverLocalSearchControllerTest {
    @Test
    void returnsProviderResultsInOriginalOrder() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://naverapihub.apigw.ntruss.com/search/v1/local"
                        + "?query=%EC%84%9C%EC%9A%B8%20%EA%B0%95%EB%82%A8%EA%B5%AC%20%EC%83%9D%EC%84%A0%EA%B5%AC%EC%9D%B4"
                        + "&display=5&start=1&sort=random&format=json"))
                .andExpect(header("X-NCP-APIGW-API-KEY-ID", "id"))
                .andExpect(header("X-NCP-APIGW-API-KEY", "secret"))
                .andRespond(withSuccess("{\"items\":[{\"title\":\"생선집\",\"category\":\"한식\"},"
                        + "{\"title\":\"구이집\",\"category\":\"생선구이\"}]}", MediaType.APPLICATION_JSON));

        var result = new NaverLocalSearchController(builder, "id", "secret")
                .search(new NaverLocalSearchController.Request("서울 강남구", "생선구이"));

        assertThat(result.configured()).isTrue();
        assertThat(result.places()).extracting(NaverLocalSearchController.Place::title)
                .containsExactly("생선집", "구이집");
        server.verify();
    }

    @Test
    void withoutKeysDoesNotCallProvider() {
        var result = new NaverLocalSearchController(RestClient.builder(), "", "")
                .search(new NaverLocalSearchController.Request("서울 강남구", "생선구이"));
        assertThat(result.configured()).isFalse();
        assertThat(result.places()).isEmpty();
    }
}

