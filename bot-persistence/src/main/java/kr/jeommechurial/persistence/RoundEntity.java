package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="lunch_rounds")
public class RoundEntity {
    @Id UUID id;
    @Column(name="owner_id",nullable=false) UUID ownerId;
    @Column(nullable=false,length=80) String title;
    @Column(name="room_name",nullable=false,length=80) String roomName;
    @Column(name="join_token_hash",nullable=false,length=64) String joinTokenHash;
    @Column(nullable=false,length=12) String status;
    @Column(length=80) String winner;
    @Version long version;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @PrePersist void insert(){id=id==null?UUID.randomUUID():id;createdAt=updatedAt=Instant.now();status=status==null?"OPEN":status;}
    @PreUpdate void update(){updatedAt=Instant.now();}
}

