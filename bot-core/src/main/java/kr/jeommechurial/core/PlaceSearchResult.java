package kr.jeommechurial.core;

public record PlaceSearchResult(String placeId, String name, String address, String category, String phone,
                                String placeUrl, double latitude, double longitude, int distanceMeters) {}
