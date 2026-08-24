package com.nightbeam.donutshards.integration.placeholder;

import com.nightbeam.donutshards.model.LeaderboardEntry;
import com.nightbeam.donutshards.transaction.TransactionService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DonutShardsExpansion extends PlaceholderExpansion {
    private static final int MAX_RANK = 10;
    private static final long CACHE_MS = 60_000L;

    private final TransactionService tx;
    private volatile List<LeaderboardEntry> cachedTop = List.of();
    private volatile long cachedAt = 0L;

    public DonutShardsExpansion(TransactionService tx) {
        this.tx = tx;
    }

    @Override
    public String getIdentifier() {
        return "donutshard";
    }

    @Override
    public String getAuthor() {
        return "NightBeam";
    }

    @Override
    public String getVersion() {
        return "1.4.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null || params.isBlank()) {
            return null;
        }
        var key = params.toLowerCase(Locale.ROOT);
        if ("balance".equals(key)) {
            if (player == null) {
                return "0";
            }
            try {
                return Long.toString(tx.balance(player.getUniqueId()).toCompletableFuture().get(2, TimeUnit.SECONDS));
            } catch (Exception ignored) {
                return "0";
            }
        }
        if (key.startsWith("top_bal_")) {
            var rank = parseRank(key.substring("top_bal_".length()));
            if (rank < 1) {
                return null;
            }
            var entry = entryAt(rank);
            return entry == null ? "" : Long.toString(entry.balance());
        }
        if (key.startsWith("top_")) {
            var rank = parseRank(key.substring("top_".length()));
            if (rank < 1) {
                return null;
            }
            var entry = entryAt(rank);
            if (entry == null) {
                return "";
            }
            var offline = Bukkit.getOfflinePlayer(entry.player());
            var name = offline.getName();
            return name == null ? entry.player().toString() : name;
        }
        return null;
    }

    private LeaderboardEntry entryAt(int rank) {
        var top = snapshotTop();
        if (rank > top.size()) {
            return null;
        }
        return top.get(rank - 1);
    }

    private List<LeaderboardEntry> snapshotTop() {
        var now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_MS && !cachedTop.isEmpty()) {
            return cachedTop;
        }
        try {
            var loaded = tx.topBalances(MAX_RANK).toCompletableFuture().get(3, TimeUnit.SECONDS);
            cachedTop = loaded;
            cachedAt = now;
            return loaded;
        } catch (Exception ignored) {
            return cachedTop;
        }
    }

    private int parseRank(String raw) {
        try {
            var rank = Integer.parseInt(raw);
            return rank >= 1 && rank <= MAX_RANK ? rank : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
