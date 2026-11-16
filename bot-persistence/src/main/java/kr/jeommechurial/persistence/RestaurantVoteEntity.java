package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="restaurant_votes",uniqueConstraints=@UniqueConstraint(name="uq_poll_voter",columnNames={"poll_date","participant_id"}))
public class RestaurantVoteEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id;
    @Column(name="poll_date",nullable=false) LocalDate pollDate;
    @Column(name="participant_id",nullable=false) UUID participantId;
    @Column(name="place_id",nullable=false) String placeId;
    @Column(name="voted_at",nullable=false) Instant votedAt;
}
