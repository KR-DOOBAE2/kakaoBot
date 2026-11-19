package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="restaurant_ratings",uniqueConstraints=@UniqueConstraint(name="uq_restaurant_rater",columnNames={"participant_id","place_id"}))
public class RestaurantRatingEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id;
    @Column(name="participant_id",nullable=false) UUID participantId;
    @Column(name="place_id",nullable=false) String placeId;
    @Column(nullable=false) boolean liked;
    @Column(nullable=false) String sentiment;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
}
