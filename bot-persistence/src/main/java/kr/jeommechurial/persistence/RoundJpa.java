package kr.jeommechurial.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoundJpa extends JpaRepository<RoundEntity, UUID> {
    List<RoundEntity> findByStatusAndDrawAtLessThanEqual(String status, Instant drawAt);
}
