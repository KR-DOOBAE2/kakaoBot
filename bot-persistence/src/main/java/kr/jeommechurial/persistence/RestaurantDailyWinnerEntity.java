package kr.jeommechurial.persistence;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
@Entity @Table(name="restaurant_daily_winners")
public class RestaurantDailyWinnerEntity {
    @Id @Column(name="poll_date") LocalDate pollDate;
    @Column(name="place_id",nullable=false) String placeId;
    @Column(name="selected_at",nullable=false) Instant selectedAt;
}
