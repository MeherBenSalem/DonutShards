package com.nightbeam.donutshards.util;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Resolves Bukkit registry entries without calling deprecated-for-removal {@code valueOf}
 * helpers. Registry {@code get} is tried first; enum/interface constants are a fallback so
 * 1.20.1 through 26.3 all resolve the same config names.
 */
public final class RegistryLookups {
    private RegistryLookups() {
    }

    public static Sound sound(String name, Sound fallback) {
        var found = lookup(registry("SOUNDS", "SOUND_EVENT"), name, Sound.class);
        return found != null ? found : fallback;
    }

    public static Particle particle(String name, Particle fallback) {
        var found = lookup(registry("PARTICLE_TYPE", "PARTICLE"), name, Particle.class);
        return found != null ? found : fallback;
    }

    public static EntityType entityType(String name) {
        return lookup(registry("ENTITY_TYPE"), name, EntityType.class);
    }

    public static Enchantment enchantment(String name) {
        var found = lookup(registry("ENCHANTMENT"), name, Enchantment.class);
        if (found != null) {
            return found;
        }
        return namedFallback(Enchantment.class, name);
    }

    public static List<String> candidateKeys(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        var trimmed = raw.trim();
        var lower = trimmed.toLowerCase(Locale.ROOT).replace(' ', '_');
        var local = lower.startsWith("minecraft:") ? lower.substring("minecraft:".length()) : lower;
        var underscored = local.replace('.', '_');
        var dotted = local.replace('_', '.');
        var keys = new LinkedHashSet<String>();
        keys.add(lower);
        keys.add(local);
        keys.add(underscored);
        keys.add(dotted);
        keys.add("minecraft:" + local);
        keys.add("minecraft:" + underscored);
        keys.add("minecraft:" + dotted);
        return List.copyOf(keys);
    }

    static String constantName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        var value = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('.', '_').replace('-', '_');
        if (value.startsWith("MINECRAFT:")) {
            value = value.substring("MINECRAFT:".length());
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Keyed> Registry<T> registry(String... fieldNames) {
        for (var fieldName : fieldNames) {
            try {
                Field field = Registry.class.getField(fieldName);
                Object value = field.get(null);
                if (value instanceof Registry<?> found) {
                    return (Registry<T>) found;
                }
            } catch (Throwable ignored) {
                // Field missing or registry not initialized on this server.
            }
        }
        return null;
    }

    private static <T extends Keyed> T lookup(Registry<T> registry, String name, Class<T> type) {
        if (name == null || name.isBlank()) {
            return null;
        }
        if (registry != null) {
            for (var candidate : candidateKeys(name)) {
                try {
                    var key = NamespacedKey.fromString(candidate.contains(":") ? candidate : "minecraft:" + candidate);
                    if (key == null) {
                        continue;
                    }
                    T got = registry.get(key);
                    if (got != null) {
                        return got;
                    }
                } catch (Throwable ignored) {
                    // get() or NamespacedKey may be unavailable during tests.
                }
            }
        }
        return namedFallback(type, name);
    }

    private static <T> T namedFallback(Class<T> type, String name) {
        var constant = constantName(name);
        if (constant.isEmpty()) {
            return null;
        }
        try {
            var method = type.getMethod("valueOf", String.class);
            return type.cast(method.invoke(null, constant));
        } catch (Throwable ignored) {
            // Sound.valueOf is deprecated-for-removal on 26.3 and may throw without a server.
        }
        try {
            return type.cast(type.getField(constant).get(null));
        } catch (Throwable ignored) {
            return null;
        }
    }
}
