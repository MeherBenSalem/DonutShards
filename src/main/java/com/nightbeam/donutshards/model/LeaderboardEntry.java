package com.nightbeam.donutshards.model;

import java.util.UUID;

/** A leaderboard row; {@code name} is the last name seen on join, or null for players not seen since 2.0. */
public record LeaderboardEntry(UUID player, long balance, String name) {
    public LeaderboardEntry(UUID player, long balance) {
        this(player, balance, null);
    }
}
