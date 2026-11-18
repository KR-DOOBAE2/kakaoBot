package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="menu_entries")
public class MenuEntity {
    @Id UUID id;
    @Column(name="round_id",nullable=false) UUID roundId;
    @Column(name="participant_id",nullable=false) UUID participantId;
    @Column(name="participant_name",nullable=false,length=40) String participantName;
    @Column(nullable=false,length=80) String menu;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    String getMenu(){return menu;}
    @PrePersist void insert(){id=id==null?UUID.randomUUID():id;createdAt=updatedAt=Instant.now();}
    @PreUpdate void update(){updatedAt=Instant.now();}
}

