package com.nightbeam.donutshards.model;
import java.util.UUID;
public record PlayerStatistics(UUID playerId, long balance, long totalEarned, long totalSpent, long afkSeconds, int rewardStreak) {}
