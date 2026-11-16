package kr.jeommechurial.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RestaurantRatingJpa extends JpaRepository<RestaurantRatingEntity,UUID> {
    Optional<RestaurantRatingEntity> findByParticipantIdAndPlaceId(UUID participantId,String placeId);
    List<RestaurantRatingEntity> findByPlaceId(String placeId);
}
