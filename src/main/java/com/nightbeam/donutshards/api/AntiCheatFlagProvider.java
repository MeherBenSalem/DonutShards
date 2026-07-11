package com.nightbeam.donutshards.api;
import java.util.UUID;
public interface AntiCheatFlagProvider { String id(); boolean isFlagged(UUID playerId); }
