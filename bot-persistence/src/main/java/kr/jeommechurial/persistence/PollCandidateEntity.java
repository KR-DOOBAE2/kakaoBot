package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="restaurant_poll_candidates",uniqueConstraints=@UniqueConstraint(name="uq_poll_place",columnNames={"poll_date","place_id"}))
public class PollCandidateEntity {
    @Id @GeneratedValue(strategy=GenerationType.UUID) UUID id;
    @Column(name="poll_date",nullable=false) LocalDate pollDate;
    @Column(name="place_id",nullable=false) String placeId;
    @Column(name="display_order",nullable=false) int displayOrder;
}
