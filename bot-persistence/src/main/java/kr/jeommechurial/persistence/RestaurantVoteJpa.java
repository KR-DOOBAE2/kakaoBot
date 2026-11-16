package kr.jeommechurial.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.*;
public interface RestaurantVoteJpa extends JpaRepository<RestaurantVoteEntity,UUID> {
    Optional<RestaurantVoteEntity> findByPollDateAndParticipantId(LocalDate date,UUID participantId);
    List<RestaurantVoteEntity> findByPollDate(LocalDate date);
}
