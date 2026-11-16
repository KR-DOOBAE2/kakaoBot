package kr.jeommechurial.service;

import kr.jeommechurial.core.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OfficeLunchServiceTest {
    @Test void ranksRecentlyDiscoveredThenVotesThenDistanceThenRating(){
        Instant now=Instant.now();
        PlaceCandidate recentFar=new PlaceCandidate("new-far","신규 원거리","", "", "", "",37.5,127,800,now,0,0);
        PlaceCandidate recentNear=new PlaceCandidate("new-near","신규 근거리","", "", "", "",37.5,127,100,now,0,0);
        PlaceCandidate oldPopular=new PlaceCandidate("old","기존 인기","", "", "", "",37.5,127,50,now.minus(Duration.ofDays(8)),10,0);
        List<PlaceCandidate> sorted=List.of(oldPopular,recentFar,recentNear).stream()
                .sorted(OfficeLunchService.ranking(Map.of("new-far",2L,"new-near",1L,"old",20L))).toList();
        assertEquals(List.of("new-far","new-near","old"),sorted.stream().map(PlaceCandidate::placeId).toList());
        List<PlaceCandidate> tiedVotes=List.of(recentFar,recentNear).stream()
                .sorted(OfficeLunchService.ranking(Map.of())).toList();
        assertEquals("new-near",tiedVotes.get(0).placeId());
    }
    @Test void savesOfficeAndBuildsRestaurantVoteForToday(){
        FakeOfficeRepository repository=new FakeOfficeRepository();FakePlaceSearch search=new FakePlaceSearch();OfficeLunchService service=new OfficeLunchService(repository,search);
        OfficeProfile office=service.configure("서울 강남구 테헤란로 1",LocalTime.of(23,59),LocalTime.MAX,1000,"admin-key");
        assertEquals(37.5,office.latitude());assertEquals(2,service.today().candidates().size());
        UUID participant=UUID.randomUUID();OfficeLunchService.PollStatus afterVote=service.vote(2,participant);
        assertEquals(1,afterVote.votes().get("place-b"));assertEquals("식당 B",afterVote.candidates().get(1).name());
    }
    @Test void rejectsVotesAfterConfiguredLunchTime(){
        FakeOfficeRepository repository=new FakeOfficeRepository();OfficeLunchService service=new OfficeLunchService(repository,new FakePlaceSearch());
        service.configure("회사 주소",LocalTime.MIDNIGHT,LocalTime.of(1,0),1000,"admin-key");
        assertThrows(IllegalStateException.class,()->service.vote(1,UUID.randomUUID()));
    }
    static class FakePlaceSearch implements RestaurantSearch {
        public Location geocode(String address){return new Location(37.5,127.0);}
        public List<PlaceSearchResult> nearbyRestaurants(double lat,double lon,int radius){return List.of(new PlaceSearchResult("place-a","식당 A","주소 A","한식","","https://map/a",lat,lon,100),new PlaceSearchResult("place-b","식당 B","주소 B","일식","","https://map/b",lat,lon,200));}
    }
    static class FakeOfficeRepository implements OfficeRepository {
        OfficeProfile profile;Map<String,PlaceCandidate> catalog=new HashMap<>();List<String> ids=new ArrayList<>();Map<String,Long> voteCounts=new HashMap<>();Map<LocalDate,String> winner=new HashMap<>();
        public Optional<OfficeProfile> profile(){return Optional.ofNullable(profile);}public void saveProfile(OfficeProfile p){profile=p;}
        public List<PlaceCandidate> discover(List<PlaceSearchResult> results){return results.stream().map(p->{PlaceCandidate candidate=new PlaceCandidate(p.placeId(),p.name(),p.address(),p.category(),p.phone(),p.placeUrl(),p.latitude(),p.longitude(),p.distanceMeters(),Instant.now(),0,0);catalog.put(p.placeId(),candidate);return candidate;}).toList();}
        public void savePoll(LocalDate date,List<String> placeIds){ids=placeIds;}
        public List<PlaceCandidate> pollCandidates(LocalDate date){return ids.stream().map(catalog::get).filter(Objects::nonNull).toList();}
        public boolean hasVoted(LocalDate date,UUID participant){return false;}
        public void vote(LocalDate date,UUID participant,String id){voteCounts.put(id,voteCounts.getOrDefault(id,0L)+1);}
        public Map<String,Long> votes(LocalDate date){return Map.copyOf(voteCounts);}
        public void rate(UUID participant,String id,boolean liked){}
        public Optional<String> winner(LocalDate date){return Optional.ofNullable(winner.get(date));}
        public void setWinner(LocalDate date,String id){winner.put(date,id);}
        public void subscribe(String key){}public void unsubscribe(String key){}public List<String> subscribers(){return List.of();}public boolean notificationSent(LocalDate date){return false;}public void markNotificationSent(LocalDate date){}
    }
}

