package com.nightbeam.donutshards.core;

import java.time.Duration;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;

/** Parses durations such as {@code 90s}, {@code 30m}, {@code 1h30m}, {@code 2d} and prints them compactly. */
public final class DurationText {
    private static final Pattern PART = Pattern.compile("(\\d+)([smhd])");

    private DurationText() {
    }

    /** Seconds in the text, or empty when it is not a positive duration. A bare number means seconds. */
    public static OptionalLong parseSeconds(String text) {
        if (text == null || text.isBlank()) {
            return OptionalLong.empty();
        }
        var value = text.trim().toLowerCase(Locale.ROOT);
        if (value.chars().allMatch(Character::isDigit)) {
            try {
                var seconds = Long.parseLong(value);
                return seconds > 0 ? OptionalLong.of(seconds) : OptionalLong.empty();
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }
        var matcher = PART.matcher(value);
        long total = 0;
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() != end) {
                return OptionalLong.empty();
            }
            end = matcher.end();
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
            var unit = switch (matcher.group(2)) {
                case "m" -> 60L;
                case "h" -> 3600L;
                case "d" -> 86400L;
                default -> 1L;
            };
            try {
                total = Math.addExact(total, Math.multiplyExact(amount, unit));
            } catch (ArithmeticException e) {
                return OptionalLong.empty();
            }
        }
        if (end != value.length() || total <= 0) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(total);
    }

    /** {@code 1h 05m}, {@code 4m 09s}, {@code 12s}; days are shown as {@code 2d 03h}. */
    public static String compact(long seconds) {
        var s = Math.max(0, seconds);
        var d = Duration.ofSeconds(s);
        if (s >= 86400) {
            return d.toDays() + "d " + pad(d.toHoursPart()) + "h";
        }
        if (s >= 3600) {
            return d.toHours() + "h " + pad(d.toMinutesPart()) + "m";
        }
        if (s >= 60) {
            return d.toMinutes() + "m " + pad(d.toSecondsPart()) + "s";
        }
        return s + "s";
    }

    private static String pad(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
