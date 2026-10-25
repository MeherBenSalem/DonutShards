package com.nightbeam.donutshards.board;

import com.nightbeam.donutshards.gui.LeaderboardGui;
import com.nightbeam.donutshards.model.LeaderboardEntry;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import com.nightbeam.donutshards.scheduler.TaskHandle;
import com.nightbeam.donutshards.service.MessageService;
import com.nightbeam.donutshards.transaction.TransactionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * In-world leaderboards: one text display entity per board showing the top balances. The entities are not saved
 * with their chunk; they are spawned and refreshed on the region thread that owns their location (Folia-safe), and
 * leftovers carrying the board tag are removed before a board respawns.
 */
public final class BoardService {
    public record Board(String name, String world, double x, double y, double z) {
    }

    private final Plugin plugin;
    private final File file;
    private final TransactionService tx;
    private final SchedulerService scheduler;
    private final MessageService messages;
    private final NamespacedKey boardKey;
    private final Map<String, Board> boards = new ConcurrentHashMap<>();
    private final Map<String, UUID> entities = new ConcurrentHashMap<>();
    private final Map<String, Integer> displayCounts = new ConcurrentHashMap<>();
    private volatile TaskHandle task;
    private volatile long refreshSeconds = 30;
    private volatile int rows = 10;
    private volatile List<LeaderboardEntry> top = List.of();

    public BoardService(Plugin plugin, File file, TransactionService tx, SchedulerService scheduler, MessageService messages) {
        this.plugin = plugin;
        this.file = file;
        this.tx = tx;
        this.scheduler = scheduler;
        this.messages = messages;
        this.boardKey = new NamespacedKey(plugin, "leaderboard-board");
        load();
    }

    public void configure(YamlConfiguration config) {
        refreshSeconds = Math.max(5, config.getLong("leaderboard.board-refresh-seconds", 30));
        rows = Math.max(1, Math.min(20, config.getInt("leaderboard.board-rows", 10)));
    }

    public void load() {
        boards.clear();
        if (!file.exists()) {
            return;
        }
        var yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("boards");
        if (section == null) {
            return;
        }
        for (var key : section.getKeys(false)) {
            var board = section.getConfigurationSection(key);
            if (board == null || board.getString("world") == null) {
                continue;
            }
            boards.put(key.toLowerCase(Locale.ROOT), new Board(key, board.getString("world"), board.getDouble("x"),
                    board.getDouble("y"), board.getDouble("z")));
        }
    }

    private void save() throws IOException {
        var yaml = new YamlConfiguration();
        yaml.set("schema-version", 1);
        yaml.createSection("boards");
        for (var board : boards.values()) {
            var path = "boards." + board.name();
            yaml.set(path + ".world", board.world());
            yaml.set(path + ".x", board.x());
            yaml.set(path + ".y", board.y());
            yaml.set(path + ".z", board.z());
        }
        yaml.save(file);
    }

    public Collection<Board> boards() {
        return boards.values().stream().sorted(Comparator.comparing(Board::name)).toList();
    }

    public Optional<Board> board(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(boards.get(name.toLowerCase(Locale.ROOT)));
    }

    public boolean create(String name, Location at) throws IOException {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,32}") || at.getWorld() == null || boards.containsKey(name.toLowerCase(Locale.ROOT))) {
            return false;
        }
        var board = new Board(name, at.getWorld().getName(), at.getX(), at.getY(), at.getZ());
        boards.put(name.toLowerCase(Locale.ROOT), board);
        save();
        refresh();
        return true;
    }

    public boolean delete(String name) throws IOException {
        var removed = boards.remove(name == null ? "" : name.toLowerCase(Locale.ROOT));
        if (removed == null) {
            return false;
        }
        save();
        var location = location(removed);
        var id = entities.remove(removed.name().toLowerCase(Locale.ROOT));
        if (location != null) {
            scheduler.region(location, () -> removeAt(location, removed.name(), id));
        }
        return true;
    }

    public void start() {
        stop();
        var period = Duration.ofSeconds(refreshSeconds);
        task = scheduler.asyncRepeating(this::refresh, Duration.ofSeconds(2), period);
    }

    public void stop() {
        var current = task;
        if (current != null) {
            current.cancel();
            task = null;
        }
    }

    /** Reloads the top balances, then updates every board on its own region thread. */
    public void refresh() {
        tx.topBalances(rows).whenComplete((entries, error) -> {
            if (error != null) {
                plugin.getLogger().log(Level.FINE, "Leaderboard boards could not load the top balances", error);
                return;
            }
            top = entries;
            for (var board : boards.values()) {
                var location = location(board);
                if (location != null) {
                    scheduler.region(location, () -> update(board, location));
                }
            }
        });
    }

    private Location location(Board board) {
        var world = Bukkit.getWorld(board.world());
        return world == null ? null : new Location(world, board.x(), board.y(), board.z());
    }

    private void update(Board board, Location location) {
        if (!location.isChunkLoaded()) {
            displayCounts.remove(board.name().toLowerCase(Locale.ROOT));
            return;
        }
        var key = board.name().toLowerCase(Locale.ROOT);
        var known = entities.get(key);
        var entity = known == null ? null : location.getWorld().getEntity(known);
        TextDisplay display = entity instanceof TextDisplay existing && existing.isValid() ? existing : null;
        if (display == null) {
            removeAt(location, board.name(), null);
            display = location.getWorld().spawn(location, TextDisplay.class, spawned -> {
                spawned.setPersistent(false);
                spawned.setBillboard(Display.Billboard.CENTER);
                spawned.setShadowed(true);
                spawned.setLineWidth(260);
                spawned.setBackgroundColor(Color.fromARGB(160, 24, 10, 40));
                spawned.getPersistentDataContainer().set(boardKey, PersistentDataType.STRING, board.name());
            });
            entities.put(key, display.getUniqueId());
        }
        display.text(render());
        var count = 0;
        for (var nearby : location.getWorld().getNearbyEntitiesByType(TextDisplay.class, location, 2.0)) {
            if (board.name().equals(nearby.getPersistentDataContainer().get(boardKey, PersistentDataType.STRING))) {
                count++;
            }
        }
        displayCounts.put(key, count);
    }

    /** Displays found at the board on its last refresh (1 when healthy), or -1 when its chunk was not loaded. */
    public int displayCount(String name) {
        return displayCounts.getOrDefault(name.toLowerCase(Locale.ROOT), -1);
    }

    Component render() {
        var lines = Component.text().append(messages.renderKey("board-title", Map.of()));
        var entries = top;
        if (entries.isEmpty()) {
            lines.append(Component.newline()).append(messages.renderKey("board-empty", Map.of()));
        }
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            lines.append(Component.newline()).append(messages.renderKey(i < 3 ? "board-entry-top" : "board-entry", Map.of(
                    "rank", Integer.toString(i + 1),
                    "player", LeaderboardGui.displayName(entry),
                    "balance", String.format(Locale.ROOT, "%,d", entry.balance()))));
        }
        lines.append(Component.newline()).append(messages.renderKey("board-footer", Map.of()));
        return lines.build();
    }

    /** Removes this board's display (by id, and any tagged leftover within two blocks). */
    private void removeAt(Location location, String name, UUID known) {
        if (!location.isChunkLoaded()) {
            return;
        }
        if (known != null) {
            var entity = location.getWorld().getEntity(known);
            if (entity != null) {
                entity.remove();
            }
        }
        for (var nearby : location.getWorld().getNearbyEntitiesByType(TextDisplay.class, location, 2.0)) {
            var tag = nearby.getPersistentDataContainer().get(boardKey, PersistentDataType.STRING);
            if (name.equals(tag)) {
                nearby.remove();
            }
        }
    }

    /** On shutdown: the displays are not saved with the chunk, so a stopping server drops them by itself. */
    public void shutdown() {
        stop();
        entities.clear();
    }
}
