package com.nightbeam.donutshards.database;
public record DatabaseConfig(Type type, String sqliteFile, String host, int port, String database, String username, String password, int poolSize, long timeoutMs) {
    public enum Type { SQLITE, MARIADB }
}
