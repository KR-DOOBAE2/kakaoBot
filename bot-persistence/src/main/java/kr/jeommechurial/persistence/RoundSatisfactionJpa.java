package kr.jeommechurial.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundSatisfactionJpa extends JpaRepository<RoundSatisfactionEntity,UUID> {
    Optional<RoundSatisfactionEntity> findByRoundIdAndRatedOnAndParticipantId(UUID roundId, LocalDate ratedOn, UUID participantId);
    List<RoundSatisfactionEntity> findByRoundIdAndRatedOn(UUID roundId, LocalDate ratedOn);
}
