package kr.jeommechurial.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RoundJpa extends JpaRepository<RoundEntity, UUID> {}
