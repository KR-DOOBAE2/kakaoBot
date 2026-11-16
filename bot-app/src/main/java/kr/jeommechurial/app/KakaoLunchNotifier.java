package kr.jeommechurial.app;

import com.fasterxml.jackson.databind.JsonNode;
import kr.jeommechurial.core.PlaceCandidate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
public class KakaoLunchNotifier {
    private final RestClient client; private final String restKey; private final String botId; private final String eventName;
    public KakaoLunchNotifier(RestClient.Builder builder,@Value("${app.kakao-rest-api-key:}") String restKey,@Value("${app.kakao-bot-id:}") String botId,@Value("${app.kakao-lunch-event-name:lunch_recommendation}") String eventName){this.client=builder.baseUrl("https://bot-api.kakao.com").build();this.restKey=restKey;this.botId=botId;this.eventName=eventName;}
    public boolean ready(){return !restKey.isBlank()&&!botId.isBlank();}
    public boolean send(PlaceCandidate place,List<String> userKeys){if(!ready()||userKeys.isEmpty())return false;boolean accepted=true;for(int start=0;start<userKeys.size();start+=100){List<Map<String,String>> users=userKeys.subList(start,Math.min(start+100,userKeys.size())).stream().map(id->Map.of("type","botUserKey","id",id)).toList();Map<String,Object> payload=Map.of("event",Map.of("name",eventName,"data",Map.of("restaurantName",place.name(),"address",place.address(),"distance",Integer.toString(place.distanceMeters()),"placeUrl",place.placeUrl(),"newlyDiscovered",Boolean.toString(place.newlyDiscovered()))),"user",users);JsonNode response=client.post().uri("/v2/bots/{id}/talk",botId).header("Authorization","KakaoAK "+restKey).body(payload).retrieve().body(JsonNode.class);accepted &= response!=null&&"SUCCESS".equals(response.path("status").asText());}return accepted;}
}
