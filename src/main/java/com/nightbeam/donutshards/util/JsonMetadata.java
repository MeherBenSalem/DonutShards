package com.nightbeam.donutshards.util;
import java.util.Map;
public final class JsonMetadata {
    private JsonMetadata() {}
    public static String encode(Map<String,String> values) {
        var out = new StringBuilder("{"); var first = true;
        for (var entry : values.entrySet()) { if (!first) out.append(','); first=false; out.append('"').append(escape(entry.getKey())).append("\":\"").append(escape(entry.getValue())).append('"'); }
        return out.append('}').toString();
    }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
