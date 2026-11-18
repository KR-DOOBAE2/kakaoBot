package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.JsonNode;
import kr.jeommechurial.core.PlaceSearchResult;
import kr.jeommechurial.service.RestaurantSearch;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import java.util.*;

@Component
@ConditionalOnProperty(name="app.place-provider",havingValue="kakao")
public class KakaoLocalAdapter implements RestaurantSearch, AddressLookup {
    private final RestClient client;
    private final String restKey;
    public KakaoLocalAdapter(RestClient.Builder builder,@Value("${app.kakao-rest-api-key:}") String restKey){this.client=builder.baseUrl("https://dapi.kakao.com").build();this.restKey=restKey;}
    @Override public Location geocode(String address){JsonNode root=get("/v2/local/search/address.json",b->b.queryParam("query",address));JsonNode docs=root.path("documents");if(!docs.isArray()||docs.isEmpty())throw new IllegalArgumentException("주소를 찾지 못했어요. 도로명 주소나 건물 이름을 확인해주세요.");JsonNode p=docs.get(0);return new Location(number(p.path("y").asText()),number(p.path("x").asText()));}
    @Override public List<AddressResult> searchAddresses(String query){JsonNode root=get("/v2/local/search/address.json",b->b.queryParam("query",query).queryParam("size",5));List<AddressResult> found=new ArrayList<>();for(JsonNode item:root.path("documents")){String address=first(item.path("road_address").path("address_name").asText(),item.path("address_name").asText());if(!address.isBlank())found.add(new AddressResult(address,number(item.path("y").asText()),number(item.path("x").asText())));}return found;}
    @Override public List<PlaceSearchResult> nearbyRestaurants(double latitude,double longitude,int radius){List<PlaceSearchResult> out=new ArrayList<>();for(int page=1;page<=3;page++){int p=page;JsonNode root=get("/v2/local/search/category.json",b->b.queryParam("category_group_code","FD6").queryParam("x",longitude).queryParam("y",latitude).queryParam("radius",radius).queryParam("sort","distance").queryParam("size",15).queryParam("page",p));JsonNode docs=root.path("documents");if(!docs.isArray())break;for(JsonNode d:docs){out.add(new PlaceSearchResult(d.path("id").asText(),d.path("place_name").asText(),first(d.path("road_address_name").asText(),d.path("address_name").asText()),d.path("category_name").asText(),d.path("phone").asText(),d.path("place_url").asText(),number(d.path("y").asText()),number(d.path("x").asText()),integer(d.path("distance").asText())));}if(root.path("meta").path("is_end").asBoolean(true))break;}return out;}
    private JsonNode get(String path,java.util.function.Consumer<UriBuilder> query){if(restKey.isBlank())throw new IllegalStateException("장소 검색을 사용하려면 KAKAO_REST_API_KEY 설정이 필요해요.");return client.get().uri(b->{b.path(path);query.accept(b);return b.build();}).header("Authorization","KakaoAK "+restKey).retrieve().body(JsonNode.class);}
    private static String first(String a,String b){return a==null||a.isBlank()?b:a;}
    private static double number(String v){try{return Double.parseDouble(v);}catch(Exception e){return 0;}}
    private static int integer(String v){try{return Integer.parseInt(v);}catch(Exception e){return 0;}}
}

