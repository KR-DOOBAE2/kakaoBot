package kr.jeommechurial.core;

import java.util.UUID;

public record CreatedRound(UUID id, UUID ownerId, String title, String roomName, String joinToken, String ownerToken) {}
