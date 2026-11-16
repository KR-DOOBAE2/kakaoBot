package kr.jeommechurial.service;

import kr.jeommechurial.core.OfficeProfile;
import kr.jeommechurial.core.PlaceCandidate;
import kr.jeommechurial.core.PlaceSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OfficeLunchService {
    private static final ZoneId ZONE=ZoneId.of("Asia/Seoul");
    private final OfficeRepository repository; private final RestaurantSearch search;
    public OfficeLunchService(OfficeRepository repository,RestaurantSearch search){this.repository=repository;this.search=search;}
    public boolean configured(){return repository.profile().isPresent();}
    @Transactional public OfficeProfile configure(String address,LocalTime lunchTime,LocalTime lunchEndTime,int radius,String adminBotUserKey){if(address==null||address.isBlank())throw new IllegalArgumentException("회사 주소를 입력해주세요.");if(lunchTime==null||lunchEndTime==null||!lunchEndTime.isAfter(lunchTime))throw new IllegalArgumentException("점심 종료 시간은 시작 시간보다 늦어야 해요.");if(radius<100||radius>20000)throw new IllegalArgumentException("검색 반경은 100~20000미터로 설정해주세요.");if(adminBotUserKey==null||adminBotUserKey.isBlank())throw new IllegalArgumentException("카카오 사용자 정보를 확인할 수 없어요.");OfficeProfile existing=repository.profile().orElse(null);if(existing!=null&&!existing.adminBotUserKey().equals(adminBotUserKey))throw new SecurityException("회사 설정은 처음 등록한 관리자만 변경할 수 있어요.");RestaurantSearch.Location loc=existing!=null&&existing.address().equals(address.strip())?new RestaurantSearch.Location(existing.latitude(),existing.longitude()):search.geocode(address.strip());OfficeProfile p=new OfficeProfile(address.strip(),loc.latitude(),loc.longitude(),lunchTime,lunchEndTime,radius,adminBotUserKey);repository.saveProfile(p);ensurePoll(LocalDate.now(ZONE),p);return p;}
    @Transactional public PollStatus today(){OfficeProfile p=repository.profile().orElseThrow(()->new IllegalStateException("먼저 회사설정 12:00 13:00 회사주소 명령으로 회사 주소와 점심시간을 등록해주세요."));LocalDate date=LocalDate.now(ZONE);List<PlaceCandidate> places=ensurePoll(date,p);Map<String,Long> votes=repository.votes(date);Optional<PlaceCandidate> winner=repository.winner(date).flatMap(id->places.stream().filter(x->x.placeId().equals(id)).findFirst());return new PollStatus(p,date,places,votes,winner,LocalTime.now(ZONE).isBefore(p.lunchTime()));}
    @Transactional public PollStatus vote(int number,UUID participant){PollStatus status=today();if(!status.pollOpen())throw new IllegalStateException("오늘 점심 추천이 이미 정해졌어요. 투표는 점심시간 전에 해주세요.");if(number<1||number>status.candidates().size())throw new IllegalArgumentException("후보 번호를 확인해주세요. 1부터 "+status.candidates().size()+"까지 입력할 수 있어요.");repository.vote(status.date(),participant,status.candidates().get(number-1).placeId());return today();}
    @Transactional public PollStatus recommend(){PollStatus status=today();if(status.winner().isPresent())return status;if(LocalTime.now(ZONE).isBefore(status.profile().lunchTime()))throw new IllegalStateException("아직 투표 중이에요. 점심시간 "+status.profile().lunchTime()+" 이후에 추천 결과를 확인해주세요.");PlaceCandidate chosen=status.candidates().stream().min(ranking(status.votes())).orElseThrow(()->new IllegalStateException("주변 식당 후보를 찾지 못했어요."));repository.setWinner(status.date(),chosen.placeId());return today();}
    @Transactional public void rate(UUID participant,boolean liked){PollStatus s=today();if(LocalTime.now(ZONE).isBefore(s.profile().lunchEndTime()))throw new IllegalStateException("식사 평가는 점심 종료 시간 "+s.profile().lunchEndTime()+" 이후에 할 수 있어요.");PlaceCandidate winner=s.winner().orElseThrow(()->new IllegalStateException("먼저 오늘점심 명령으로 추천 결과를 확인해주세요."));repository.rate(participant,winner.placeId(),liked);}
    @Transactional public void subscribe(String botUserKey){if(botUserKey==null||botUserKey.isBlank())throw new IllegalArgumentException("카카오 사용자 정보를 확인할 수 없어요.");repository.subscribe(botUserKey);}
    @Transactional public void unsubscribe(String botUserKey){if(botUserKey!=null&&!botUserKey.isBlank())repository.unsubscribe(botUserKey);}
    public List<String> subscribers(){return repository.subscribers();}
    public boolean notificationSent(LocalDate date){return repository.notificationSent(date);}
    @Transactional public void markNotificationSent(LocalDate date){repository.markNotificationSent(date);}
    private List<PlaceCandidate> ensurePoll(LocalDate date,OfficeProfile profile){List<PlaceCandidate> current=repository.pollCandidates(date);if(!current.isEmpty())return current;List<PlaceSearchResult> found=search.nearbyRestaurants(profile.latitude(),profile.longitude(),profile.radiusMeters());if(found.isEmpty())throw new IllegalStateException("회사 주변에서 음식점을 찾지 못했어요. 주소나 검색 반경을 확인해주세요.");List<PlaceCandidate> discovered=repository.discover(found);List<PlaceCandidate> chosen=discovered.stream().collect(Collectors.toMap(PlaceCandidate::placeId,x->x,(a,b)->a,LinkedHashMap::new)).values().stream().sorted(ranking(Map.of())).limit(5).toList();repository.savePoll(date,chosen.stream().map(PlaceCandidate::placeId).toList());return repository.pollCandidates(date);}
    static Comparator<PlaceCandidate> ranking(Map<String,Long> votes){return Comparator.comparing(PlaceCandidate::newlyDiscovered).reversed().thenComparing(Comparator.comparingLong((PlaceCandidate p)->votes.getOrDefault(p.placeId(),0L)).reversed()).thenComparingInt(PlaceCandidate::distanceMeters).thenComparing(PlaceCandidate::score,Comparator.reverseOrder()).thenComparing(PlaceCandidate::name);}
    public record PollStatus(OfficeProfile profile,LocalDate date,List<PlaceCandidate> candidates,Map<String,Long> votes,Optional<PlaceCandidate> winner,boolean pollOpen){}
}



