package kr.jeommechurial.persistence;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
@Entity @Table(name="lunch_notification_sent")
public class LunchNotificationSentEntity {
    @Id @Column(name="lunch_date") LocalDate lunchDate;
    @Column(name="sent_at",nullable=false) Instant sentAt;
}
