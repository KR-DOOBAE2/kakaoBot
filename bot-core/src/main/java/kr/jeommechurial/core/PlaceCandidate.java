package kr.jeommechurial.core;

import java.time.Instant;

public record PlaceCandidate(String placeId, String name, String address, String category, String phone,
                             String placeUrl, double latitude, double longitude, int distanceMeters,
                             Instant firstSeenAt, int likes, int dislikes) {
    public int score() { return likes - dislikes; }
    public boolean newlyDiscovered() { return firstSeenAt != null && firstSeenAt.isAfter(Instant.now().minusSeconds(7L * 24 * 60 * 60)); }
}
