package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="round_satisfaction", uniqueConstraints=@UniqueConstraint(name="uq_round_satisfaction_participant", columnNames={"round_id","rated_on","participant_id"}))
public class RoundSatisfactionEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id;
    @Column(name="round_id",nullable=false) UUID roundId;
    @Column(name="rated_on",nullable=false) LocalDate ratedOn;
    @Column(name="participant_id",nullable=false) UUID participantId;
    @Column(nullable=false) String satisfaction;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
}
