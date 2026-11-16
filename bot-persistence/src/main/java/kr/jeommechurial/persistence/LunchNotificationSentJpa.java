package kr.jeommechurial.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
public interface LunchNotificationSentJpa extends JpaRepository<LunchNotificationSentEntity,LocalDate> {}
