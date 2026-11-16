package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity @Table(name="office_profile")
public class OfficeProfileEntity {
    @Id String id = "default";
    @Column(nullable=false,length=240) String address;
    @Column(nullable=false) double latitude;
    @Column(nullable=false) double longitude;
    @Column(name="lunch_time",nullable=false) LocalTime lunchTime;
    @Column(name="lunch_end_time",nullable=false) LocalTime lunchEndTime;
    @Column(name="radius_meters",nullable=false) int radiusMeters;
    @Column(name="admin_bot_user_key",nullable=false,length=200) String adminBotUserKey;
}

