package com.nightbeam.donutshards.core;

/** Paging for {@code /shards history}. Pages are 1-based. */
public record HistoryPage(int page, int size) {
    public static final int MAX_PAGES = 50;

    public static HistoryPage parse(String text, int size) {
        var page = 1;
        if (text != null) {
            try {
                page = Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        return new HistoryPage(Math.max(1, Math.min(MAX_PAGES, page)), Math.max(1, Math.min(20, size)));
    }

    public int offset() {
        return (page - 1) * size;
    }

    /** Ages such as {@code 12s}, {@code 5m}, {@code 3h}, {@code 2d}. */
    public static String ago(long millis) {
        var seconds = Math.max(0, millis / 1000);
        if (seconds < 60) {
            return seconds + "s";
        }
        if (seconds < 3600) {
            return seconds / 60 + "m";
        }
        if (seconds < 86400) {
            return seconds / 3600 + "h";
        }
        return seconds / 86400 + "d";
    }
}
