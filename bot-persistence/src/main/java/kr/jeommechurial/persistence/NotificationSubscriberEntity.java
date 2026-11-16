package kr.jeommechurial.persistence;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="lunch_notification_subscribers")
public class NotificationSubscriberEntity {
    @Id @Column(name="bot_user_key",length=200) String botUserKey;
    @Column(nullable=false) boolean enabled;
    @Column(name="created_at",nullable=false) Instant createdAt;
}
