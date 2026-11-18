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
    Optional<MenuEntity> findByRoundIdAndParticipantIdAndMenuIgnoreCase(UUID roundId, UUID participantId, String menu);
    @Modifying @Query(value="insert into menu_entries(id,round_id,participant_id,participant_name,menu,created_at,updated_at) values (:id,:roundId,:participantId,:name,:menu,now(),now()) on conflict do nothing",nativeQuery=true)
    int insertIfAbsent(@Param("id") UUID id,@Param("roundId") UUID roundId,@Param("participantId") UUID participantId,@Param("name") String name,@Param("menu") String menu);
    @Modifying @Query("delete from MenuEntity m where m.roundId=:roundId and m.participantId=:participantId")
    void remove(@Param("roundId") UUID roundId, @Param("participantId") UUID participantId);
    @Modifying @Query("delete from MenuEntity m where m.roundId=:roundId and m.participantId=:participantId and m.id=:entryId")
    void removeOne(@Param("roundId") UUID roundId,@Param("participantId") UUID participantId,@Param("entryId") UUID entryId);
    @Modifying @Query("delete from MenuEntity m where m.roundId=:roundId")
    void deleteByRoundId(@Param("roundId") UUID roundId);
}
