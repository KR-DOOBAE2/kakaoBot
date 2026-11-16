package kr.jeommechurial.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="restaurants")
public class RestaurantEntity {
    @Id String placeId;
    @Column(nullable=false,length=160) String name;
    @Column(nullable=false,length=300) String address;
    @Column(length=160) String category;
    @Column(length=80) String phone;
    @Column(name="place_url",length=500) String placeUrl;
    @Column(nullable=false) double latitude;
    @Column(nullable=false) double longitude;
    @Column(name="distance_meters",nullable=false) int distanceMeters;
    @Column(name="first_seen_at",nullable=false) Instant firstSeenAt;
    @Column(name="last_recommended_at") Instant lastRecommendedAt;
}
