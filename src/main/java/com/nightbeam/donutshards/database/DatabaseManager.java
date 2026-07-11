package com.nightbeam.donutshards.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.*;
import java.util.List;
import java.util.function.Function;

public final class DatabaseManager implements AutoCloseable {
    private final HikariDataSource source;
    private final DatabaseConfig.Type type;
    public DatabaseManager(DatabaseConfig config, Path dataFolder) {
        this.type = config.type(); var hc = new HikariConfig();
        if (type == DatabaseConfig.Type.SQLITE) {
            hc.setJdbcUrl("jdbc:sqlite:" + dataFolder.resolve(config.sqliteFile()).toAbsolutePath()); hc.setMaximumPoolSize(1); hc.setConnectionTestQuery("SELECT 1");
        } else {
            hc.setJdbcUrl("jdbc:mariadb://" + config.host() + ':' + config.port() + '/' + config.database()); hc.setUsername(config.username()); hc.setPassword(config.password()); hc.setMaximumPoolSize(config.poolSize());
        }
        hc.setPoolName("DonutShards-SQL"); hc.setConnectionTimeout(config.timeoutMs()); hc.setAutoCommit(true); this.source = new HikariDataSource(hc);
    }
    public DatabaseConfig.Type type() { return type; }
    public Connection connection() throws SQLException { return source.getConnection(); }
    public <T> T transaction(Function<Connection,T> work) throws SQLException {
        try (var c = connection()) { c.setAutoCommit(false); try { var value = work.apply(c); c.commit(); return value; } catch (RuntimeException e) { c.rollback(); throw e; } finally { c.setAutoCommit(true); } }
    }
    public void migrate() throws SQLException {
        try (var c = connection(); var statement = c.createStatement()) {
            for (var sql : migrations()) statement.executeUpdate(sql);
        }
    }
    private List<String> migrations() {
        var auto = type == DatabaseConfig.Type.SQLITE ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "BIGINT PRIMARY KEY AUTO_INCREMENT";
        return List.of(
            "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER PRIMARY KEY, checksum VARCHAR(64) NOT NULL, applied_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS players (uuid VARCHAR(36) PRIMARY KEY, last_name VARCHAR(16), balance BIGINT NOT NULL DEFAULT 0, total_earned BIGINT NOT NULL DEFAULT 0, total_spent BIGINT NOT NULL DEFAULT 0, last_reward BIGINT NOT NULL DEFAULT 0, reward_streak INTEGER NOT NULL DEFAULT 0, afk_seconds BIGINT NOT NULL DEFAULT 0, version BIGINT NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS transactions (id VARCHAR(36) PRIMARY KEY, correlation_id VARCHAR(36) NOT NULL, player_uuid VARCHAR(36) NOT NULL, related_uuid VARCHAR(36), type VARCHAR(32) NOT NULL, amount BIGINT NOT NULL, previous_balance BIGINT NOT NULL, new_balance BIGINT NOT NULL, source VARCHAR(128) NOT NULL, created_at BIGINT NOT NULL, server_id VARCHAR(64) NOT NULL, administrator_uuid VARCHAR(36), metadata TEXT NOT NULL, idempotency_key VARCHAR(128) NOT NULL UNIQUE, rolled_back INTEGER NOT NULL DEFAULT 0)",
            "CREATE INDEX IF NOT EXISTS idx_transactions_player_time ON transactions(player_uuid, created_at)",
            "CREATE TABLE IF NOT EXISTS purchases (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, item_id VARCHAR(128) NOT NULL, price BIGINT NOT NULL, state VARCHAR(24) NOT NULL, idempotency_key VARCHAR(128) NOT NULL UNIQUE, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS reward_history (id " + auto + ", player_uuid VARCHAR(36) NOT NULL, reward_id VARCHAR(128) NOT NULL, amount BIGINT NOT NULL, created_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS afk_sessions (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, zone_id VARCHAR(128) NOT NULL, started_at BIGINT NOT NULL, ended_at BIGINT)",
            "CREATE TABLE IF NOT EXISTS shop_limits (scope_key VARCHAR(255) PRIMARY KEY, count BIGINT NOT NULL, resets_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS audit_logs (id " + auto + ", category VARCHAR(64) NOT NULL, player_uuid VARCHAR(36), detail TEXT NOT NULL, created_at BIGINT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS permission_grants (id VARCHAR(36) PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, permission VARCHAR(255) NOT NULL, expires_at BIGINT, purchase_id VARCHAR(36))",
            "CREATE TABLE IF NOT EXISTS network_fingerprints (fingerprint VARCHAR(128) NOT NULL, player_uuid VARCHAR(36) NOT NULL, last_seen BIGINT NOT NULL, PRIMARY KEY(fingerprint, player_uuid))"
        );
    }
    public void close() { source.close(); }
}
