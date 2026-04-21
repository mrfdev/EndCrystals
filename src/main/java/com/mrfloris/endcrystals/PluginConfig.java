package com.mrfloris.endcrystals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public record PluginConfig(
        String localeName,
        List<String> commandAliases,
        boolean preventBlockDamage,
        boolean preventPlayerBreak,
        boolean preventProjectileBreak,
        boolean preventProtectedEntityDamage,
        boolean allowPlayerBreakInTheEnd,
        boolean clearExplosionYield,
        boolean logBlockProtection,
        boolean logCrystalBreaks,
        List<String> liveToggleKeys,
        Set<EntityType> protectedEntityTypes,
        String prefix,
        Map<String, String> messages
) {

    private static final List<String> DEFAULT_COMMAND_ALIASES = List.of(
            "endcrystals",
            "ec"
    );

    private static final List<String> DEFAULT_LIVE_TOGGLES = List.of(
            "protection.prevent-block-damage",
            "protection.prevent-player-break",
            "protection.prevent-projectile-break",
            "protection.prevent-protected-entity-damage",
            "protection.allow-player-break-in-the-end",
            "protection.clear-explosion-yield",
            "debug.log-block-protection",
            "debug.log-crystal-breaks"
    );

    private static final List<String> DEFAULT_PROTECTED_ENTITY_TYPES = List.of(
            "ARMOR_STAND",
            "ITEM",
            "ITEM_FRAME",
            "GLOW_ITEM_FRAME",
            "PAINTING",
            "ITEM_DISPLAY",
            "BLOCK_DISPLAY",
            "TEXT_DISPLAY",
            "LEASH_KNOT",
            "MINECART",
            "CHEST_MINECART",
            "COMMAND_BLOCK_MINECART",
            "FURNACE_MINECART",
            "HOPPER_MINECART",
            "SPAWNER_MINECART",
            "TNT_MINECART",
            "ACACIA_BOAT",
            "ACACIA_CHEST_BOAT",
            "BAMBOO_RAFT",
            "BAMBOO_CHEST_RAFT",
            "BIRCH_BOAT",
            "BIRCH_CHEST_BOAT",
            "CHERRY_BOAT",
            "CHERRY_CHEST_BOAT",
            "DARK_OAK_BOAT",
            "DARK_OAK_CHEST_BOAT",
            "JUNGLE_BOAT",
            "JUNGLE_CHEST_BOAT",
            "MANGROVE_BOAT",
            "MANGROVE_CHEST_BOAT",
            "OAK_BOAT",
            "OAK_CHEST_BOAT",
            "PALE_OAK_BOAT",
            "PALE_OAK_CHEST_BOAT",
            "SPRUCE_BOAT",
            "SPRUCE_CHEST_BOAT"
    );

    public static PluginConfig from(YamlConfiguration yaml, YamlConfiguration localeYaml, String localeName) {
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("no-permission", localeYaml.getString("messages.no-permission", "<red>You do not have permission to do that.</red>"));
        messages.put("reloaded", localeYaml.getString("messages.reloaded", "<green>Configuration reloaded from <white>%path%</white>.</green>"));
        messages.put("toggle-updated", localeYaml.getString("messages.toggle-updated", "<green><white>%setting%</white> is now <white>%value%</white>.</green>"));
        messages.put("unknown-setting", localeYaml.getString("messages.unknown-setting", "<red>Unknown toggle: <white>%setting%</white></red>"));
        messages.put("command-usage", localeYaml.getString("messages.command-usage", "<yellow>Use <white>/_endcrystals debug</white>, <white>reload</white>, or <white>toggle [setting] [true|false]</white>.</yellow>"));
        messages.put("player-break-blocked", localeYaml.getString("messages.player-break-blocked", "<red>These end crystals are protected.</red>"));

        List<String> configuredProtectedTypes = yaml.getStringList("protection.protected-entity-types");
        if (configuredProtectedTypes.isEmpty()) {
            configuredProtectedTypes = DEFAULT_PROTECTED_ENTITY_TYPES;
        }

        return new PluginConfig(
                localeName,
                parseCommandAliases(yaml.isList("commands.aliases")
                        ? yaml.getStringList("commands.aliases")
                        : DEFAULT_COMMAND_ALIASES),
                yaml.getBoolean("protection.prevent-block-damage", true),
                yaml.getBoolean("protection.prevent-player-break", true),
                yaml.getBoolean("protection.prevent-projectile-break", true),
                yaml.getBoolean("protection.prevent-protected-entity-damage", true),
                yaml.getBoolean("protection.allow-player-break-in-the-end", false),
                yaml.getBoolean("protection.clear-explosion-yield", true),
                yaml.getBoolean("debug.log-block-protection", false),
                yaml.getBoolean("debug.log-crystal-breaks", false),
                List.copyOf(yaml.getStringList("live-toggles").isEmpty() ? DEFAULT_LIVE_TOGGLES : yaml.getStringList("live-toggles")),
                parseProtectedEntityTypes(configuredProtectedTypes),
                localeYaml.getString("messages.prefix", "<gray>[<gold>1MB-EndCrystals</gold>]</gray> "),
                Map.copyOf(messages)
        );
    }

    public String message(String key) {
        return messages.getOrDefault(key, "<red>Missing message for key: <white>" + key + "</white></red>");
    }

    public boolean protectsEntity(Entity entity) {
        return protectedEntityTypes.contains(entity.getType());
    }

    public String protectedEntityTypesDisplay() {
        return protectedEntityTypes.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    public String commandAliasesDisplay() {
        return commandAliases.isEmpty() ? "None" : String.join(", ", commandAliases);
    }

    private static List<String> parseCommandAliases(List<String> configuredAliases) {
        return configuredAliases.stream()
                .map(alias -> alias == null ? "" : alias.trim().toLowerCase(java.util.Locale.ROOT))
                .map(alias -> alias.startsWith("/") ? alias.substring(1) : alias)
                .filter(alias -> !alias.isBlank())
                .filter(alias -> !alias.contains(" "))
                .filter(alias -> !"_endcrystals".equals(alias))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }

    private static Set<EntityType> parseProtectedEntityTypes(List<String> configuredTypes) {
        return configuredTypes.stream()
                .map(type -> type.toUpperCase(java.util.Locale.ROOT))
                .flatMap(type -> Arrays.stream(resolveEntityType(type)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static EntityType[] resolveEntityType(String type) {
        try {
            return new EntityType[]{EntityType.valueOf(type)};
        } catch (IllegalArgumentException ignored) {
            return new EntityType[0];
        }
    }
}
