package com.mrfloris.endcrystals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;

public record PluginConfig(
        boolean preventBlockDamage,
        boolean preventPlayerBreak,
        boolean preventProjectileBreak,
        boolean allowPlayerBreakInTheEnd,
        boolean clearExplosionYield,
        boolean logBlockProtection,
        boolean logCrystalBreaks,
        List<String> liveToggleKeys,
        String prefix,
        Map<String, String> messages
) {

    private static final List<String> DEFAULT_LIVE_TOGGLES = List.of(
            "protection.prevent-block-damage",
            "protection.prevent-player-break",
            "protection.prevent-projectile-break",
            "protection.allow-player-break-in-the-end",
            "protection.clear-explosion-yield",
            "debug.log-block-protection",
            "debug.log-crystal-breaks"
    );

    public static PluginConfig from(YamlConfiguration yaml) {
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("no-permission", yaml.getString("messages.no-permission", "<red>You do not have permission to do that.</red>"));
        messages.put("reloaded", yaml.getString("messages.reloaded", "<green>Configuration reloaded from <white>%path%</white>.</green>"));
        messages.put("toggle-updated", yaml.getString("messages.toggle-updated", "<green><white>%setting%</white> is now <white>%value%</white>.</green>"));
        messages.put("unknown-setting", yaml.getString("messages.unknown-setting", "<red>Unknown toggle: <white>%setting%</white></red>"));
        messages.put("command-usage", yaml.getString("messages.command-usage", "<yellow>Use <white>/_endcrystals debug</white>, <white>reload</white>, or <white>toggle &lt;setting&gt; [true|false]</white>.</yellow>"));
        messages.put("player-break-blocked", yaml.getString("messages.player-break-blocked", "<red>These end crystals are protected.</red>"));

        return new PluginConfig(
                yaml.getBoolean("protection.prevent-block-damage", true),
                yaml.getBoolean("protection.prevent-player-break", true),
                yaml.getBoolean("protection.prevent-projectile-break", true),
                yaml.getBoolean("protection.allow-player-break-in-the-end", true),
                yaml.getBoolean("protection.clear-explosion-yield", true),
                yaml.getBoolean("debug.log-block-protection", false),
                yaml.getBoolean("debug.log-crystal-breaks", false),
                List.copyOf(yaml.getStringList("live-toggles").isEmpty() ? DEFAULT_LIVE_TOGGLES : yaml.getStringList("live-toggles")),
                yaml.getString("messages.prefix", "<gray>[<gold>1MB-EndCrystals</gold>]</gray> "),
                Map.copyOf(messages)
        );
    }

    public String message(String key) {
        return messages.getOrDefault(key, "<red>Missing message for key: <white>" + key + "</white></red>");
    }
}
