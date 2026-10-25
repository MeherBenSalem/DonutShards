package com.nightbeam.donutshards.integration.placeholder;

import com.nightbeam.donutshards.booster.BoosterService;
import com.nightbeam.donutshards.core.BoosterState;
import com.nightbeam.donutshards.core.DurationText;
import com.nightbeam.donutshards.gui.LeaderboardGui;
import com.nightbeam.donutshards.model.LeaderboardEntry;
import com.nightbeam.donutshards.reward.RewardService;
import com.nightbeam.donutshards.transaction.TransactionService;
import com.nightbeam.donutshards.zone.ZoneService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@code %donutshard_*%}. Nothing here touches the database on the calling thread: balances come from the cache
 * (refreshed in the background when missing) and the top list from a snapshot reloaded asynchronously.
 */
public final class DonutShardsExpansion extends PlaceholderExpansion {
    private static final int MAX_RANK = 10;

    private final TransactionService tx;
    private final ZoneService zones;
    private final RewardService rewards;
    private final BoosterService boosters;
    private final String version;
    private final AtomicLong cacheMillis;
    private volatile List<LeaderboardEntry> cachedTop = List.of();
    private volatile long cachedAt = 0L;
    private final AtomicBoolean loadingTop = new AtomicBoolean();

    public DonutShardsExpansion(TransactionService tx, ZoneService zones, RewardService rewards, BoosterService boosters,
                                String version, AtomicLong cacheMillis) {
        this.tx = tx;
        this.zones = zones;
        this.rewards = rewards;
        this.boosters = boosters;
        this.version = version;
        this.cacheMillis = cacheMillis;
    }

    @Override
    public String getIdentifier() {
        return "donutshard";
    }

    @Override
    public String getAuthor() {
        return "NightBeam Studio";
    }

    @Override
    public String getVersion() {
        return version;
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
        switch (key) {
            case "balance" -> {
                if (player == null) {
                    return "0";
                }
                var cached = tx.cached(player.getUniqueId());
                if (cached.isPresent()) {
                    return Long.toString(cached.getAsLong());
                }
                tx.balance(player.getUniqueId());
                return "0";
            }
            case "zone" -> {
                return player == null ? "" : zones.current(player.getUniqueId()).map(zones::label).orElse("");
            }
            case "next_reward" -> {
                if (player == null) {
                    return "";
                }
                var seconds = rewards.secondsToNext(player.getUniqueId());
                return seconds < 0 ? "" : Long.toString(seconds);
            }
            case "session" -> {
                return player == null ? "0" : rewards.session(player.getUniqueId()).map(s -> Long.toString(s.earned())).orElse("0");
            }
            case "booster" -> {
                return BoosterState.format(boosters.multiplier());
            }
            case "booster_time" -> {
                return boosters.active().map(b -> DurationText.compact(b.secondsLeft(System.currentTimeMillis()))).orElse("");
            }
            default -> {
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
            return entry == null ? "" : LeaderboardGui.displayName(entry);
        }
        return null;
    }

    private LeaderboardEntry entryAt(int rank) {
        refreshTopIfStale();
        var top = cachedTop;
        return rank > top.size() ? null : top.get(rank - 1);
    }

    private void refreshTopIfStale() {
        var now = System.currentTimeMillis();
        if (now - cachedAt < cacheMillis.get() || !loadingTop.compareAndSet(false, true)) {
            return;
        }
        tx.topBalances(MAX_RANK).whenComplete((loaded, error) -> {
            if (error == null) {
                cachedTop = List.copyOf(loaded);
                cachedAt = System.currentTimeMillis();
            }
            loadingTop.set(false);
        });
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
