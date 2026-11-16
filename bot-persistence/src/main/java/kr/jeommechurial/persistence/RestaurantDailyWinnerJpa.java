package kr.jeommechurial.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
public interface RestaurantDailyWinnerJpa extends JpaRepository<RestaurantDailyWinnerEntity,LocalDate> {}
