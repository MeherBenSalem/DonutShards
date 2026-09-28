package com.nightbeam.donutshards.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Resolves Bukkit registry entries without calling deprecated-for-removal {@code valueOf}
 * helpers. Registry {@code get} is tried first; enum/interface constants are a fallback so
 * 1.20.1 through 26.3 all resolve the same config names.
 *
 * <p>Lookups are not typed as {@code Keyed} because {@code Particle} is not {@code Keyed} on
 * 1.20.1 even though it is on later Paper APIs.
 */
public final class RegistryLookups {
    private RegistryLookups() {
    }

    public static Sound sound(String name, Sound fallback) {
        var found = lookup(name, Sound.class, "SOUNDS", "SOUND_EVENT");
        return found != null ? found : fallback;
    }

    public static Particle particle(String name, Particle fallback) {
        var found = lookup(name, Particle.class, "PARTICLE_TYPE", "PARTICLE");
        return found != null ? found : fallback;
    }

    public static EntityType entityType(String name) {
        return lookup(name, EntityType.class, "ENTITY_TYPE");
    }

    public static Enchantment enchantment(String name) {
        return lookup(name, Enchantment.class, "ENCHANTMENT");
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

    private static Object registry(String... fieldNames) {
        for (var fieldName : fieldNames) {
            try {
                Field field = Registry.class.getField(fieldName);
                Object value = field.get(null);
                if (value != null) {
                    return value;
                }
            } catch (Throwable ignored) {
                // Field missing or registry not initialized on this server.
            }
        }
        return null;
    }

    private static <T> T lookup(String name, Class<T> type, String... registryFields) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Object registry = registry(registryFields);
        if (registry != null) {
            Method get = null;
            try {
                get = registry.getClass().getMethod("get", NamespacedKey.class);
            } catch (Throwable ignored) {
                try {
                    get = Registry.class.getMethod("get", NamespacedKey.class);
                } catch (Throwable ignored2) {
                    get = null;
                }
            }
            if (get != null) {
                for (var candidate : candidateKeys(name)) {
                    try {
                        var key = NamespacedKey.fromString(candidate.contains(":") ? candidate : "minecraft:" + candidate);
                        if (key == null) {
                            continue;
                        }
                        Object got = get.invoke(registry, key);
                        if (type.isInstance(got)) {
                            return type.cast(got);
                        }
                    } catch (Throwable ignored) {
                        // get() or NamespacedKey may be unavailable during tests / old servers.
                    }
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
            Object value = method.invoke(null, constant);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        } catch (Throwable ignored) {
            // Sound.valueOf is deprecated-for-removal on 26.3 and may throw without a server.
        }
        try {
            Object value = type.getField(constant).get(null);
            if (type.isInstance(value)) {
                return type.cast(value);
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }
}
