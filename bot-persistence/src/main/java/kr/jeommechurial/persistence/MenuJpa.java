package kr.jeommechurial.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuJpa extends JpaRepository<MenuEntity, UUID> {
    List<MenuEntity> findByRoundIdOrderByCreatedAtAsc(UUID id);
    Optional<MenuEntity> findByRoundIdAndParticipantId(UUID roundId, UUID participantId);
    @Modifying @Query("delete from MenuEntity m where m.roundId=:roundId and m.participantId=:participantId")
    void remove(@Param("roundId") UUID roundId, @Param("participantId") UUID participantId);
}
