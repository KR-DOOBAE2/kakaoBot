package kr.jeommechurial.core;

import java.time.LocalTime;

public record OfficeProfile(String address, double latitude, double longitude, LocalTime lunchTime, LocalTime lunchEndTime, int radiusMeters, String adminBotUserKey) {}
