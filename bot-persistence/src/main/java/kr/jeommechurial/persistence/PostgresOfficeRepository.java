package kr.jeommechurial.persistence;

import kr.jeommechurial.core.*;
import kr.jeommechurial.service.OfficeRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository @Transactional
class PostgresOfficeRepository implements OfficeRepository {
    private final OfficeProfileJpa profiles; private final RestaurantJpa restaurants; private final PollCandidateJpa pollCandidates;
    private final RestaurantVoteJpa votes; private final RestaurantRatingJpa ratings; private final RestaurantDailyWinnerJpa winners; private final NotificationSubscriberJpa subscribers; private final LunchNotificationSentJpa notifications;
    PostgresOfficeRepository(OfficeProfileJpa profiles,RestaurantJpa restaurants,PollCandidateJpa pollCandidates,RestaurantVoteJpa votes,RestaurantRatingJpa ratings,RestaurantDailyWinnerJpa winners,NotificationSubscriberJpa subscribers,LunchNotificationSentJpa notifications){this.profiles=profiles;this.restaurants=restaurants;this.pollCandidates=pollCandidates;this.votes=votes;this.ratings=ratings;this.winners=winners;this.subscribers=subscribers;this.notifications=notifications;}
    @Transactional(readOnly=true) public Optional<OfficeProfile> profile(){return profiles.findById("default").map(e->new OfficeProfile(e.address,e.latitude,e.longitude,e.lunchTime,e.lunchEndTime,e.radiusMeters,e.adminBotUserKey));}
    public void saveProfile(OfficeProfile p){OfficeProfileEntity e=profiles.findById("default").orElseGet(OfficeProfileEntity::new);e.id="default";e.address=p.address();e.latitude=p.latitude();e.longitude=p.longitude();e.lunchTime=p.lunchTime();e.lunchEndTime=p.lunchEndTime();e.radiusMeters=p.radiusMeters();e.adminBotUserKey=p.adminBotUserKey();profiles.save(e);}
    public List<PlaceCandidate> discover(List<PlaceSearchResult> places){List<PlaceCandidate> result=new ArrayList<>();Instant now=Instant.now();for(PlaceSearchResult p:places){RestaurantEntity e=restaurants.findById(p.placeId()).orElseGet(()->{RestaurantEntity n=new RestaurantEntity();n.placeId=p.placeId();n.firstSeenAt=now;return n;});e.name=p.name();e.address=p.address();e.category=p.category();e.phone=p.phone();e.placeUrl=p.placeUrl();e.latitude=p.latitude();e.longitude=p.longitude();e.distanceMeters=p.distanceMeters();restaurants.save(e);List<RestaurantRatingEntity> rs=ratings.findByPlaceId(p.placeId());int likes=(int)rs.stream().filter(x->x.liked).count(),dislikes=rs.size()-likes;result.add(model(e,likes,dislikes));}return result;}
    public void savePoll(LocalDate date,List<String> ids){pollCandidates.deleteByPollDate(date);int order=0;for(String id:ids){PollCandidateEntity e=new PollCandidateEntity();e.pollDate=date;e.placeId=id;e.displayOrder=order++;pollCandidates.save(e);}}
    @Transactional(readOnly=true) public List<PlaceCandidate> pollCandidates(LocalDate date){return pollCandidates.findByPollDateOrderByDisplayOrderAsc(date).stream().map(p->restaurants.findById(p.placeId).map(e->{List<RestaurantRatingEntity> rs=ratings.findByPlaceId(p.placeId);int likes=(int)rs.stream().filter(x->x.liked).count();return model(e,likes,rs.size()-likes);}).orElse(null)).filter(Objects::nonNull).toList();}
    @Transactional(readOnly=true) public boolean hasVoted(LocalDate date,UUID participant){return votes.findByPollDateAndParticipantId(date,participant).isPresent();}
    public void vote(LocalDate date,UUID participant,String placeId){RestaurantVoteEntity e=votes.findByPollDateAndParticipantId(date,participant).orElseGet(RestaurantVoteEntity::new);e.pollDate=date;e.participantId=participant;e.placeId=placeId;e.votedAt=Instant.now();votes.save(e);}
    @Transactional(readOnly=true) public Map<String,Long> votes(LocalDate date){return votes.findByPollDate(date).stream().collect(Collectors.groupingBy(v->v.placeId,Collectors.counting()));}
    public void rate(UUID participant,String placeId,boolean liked){RestaurantRatingEntity e=ratings.findByParticipantIdAndPlaceId(participant,placeId).orElseGet(RestaurantRatingEntity::new);e.participantId=participant;e.placeId=placeId;e.liked=liked;e.updatedAt=Instant.now();ratings.save(e);}
    @Transactional(readOnly=true) public Optional<String> winner(LocalDate date){return winners.findById(date).map(e->e.placeId);}
    public void setWinner(LocalDate date,String placeId){RestaurantDailyWinnerEntity e=winners.findById(date).orElseGet(RestaurantDailyWinnerEntity::new);e.pollDate=date;e.placeId=placeId;e.selectedAt=Instant.now();winners.save(e);}
    public void subscribe(String key){NotificationSubscriberEntity e=subscribers.findById(key).orElseGet(NotificationSubscriberEntity::new);e.botUserKey=key;e.enabled=true;e.createdAt=Instant.now();subscribers.save(e);}
    public void unsubscribe(String key){subscribers.deleteById(key);}
    @Transactional(readOnly=true) public List<String> subscribers(){return subscribers.findAll().stream().filter(e->e.enabled).map(e->e.botUserKey).toList();}
    @Transactional(readOnly=true) public boolean notificationSent(LocalDate date){return notifications.existsById(date);}
    public void markNotificationSent(LocalDate date){LunchNotificationSentEntity e=new LunchNotificationSentEntity();e.lunchDate=date;e.sentAt=Instant.now();notifications.save(e);}
    private static PlaceCandidate model(RestaurantEntity e,int likes,int dislikes){return new PlaceCandidate(e.placeId,e.name,e.address,e.category,e.phone,e.placeUrl,e.latitude,e.longitude,e.distanceMeters,e.firstSeenAt,likes,dislikes);}
}




