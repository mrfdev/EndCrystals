package com.mrfloris.endcrystals;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ExternalConfigManager {

    private final EndCrystalsPlugin plugin;
    private final Path configDirectory;
    private final Path configPath;
    private final Path legacyConfigPath;

    private YamlConfiguration yaml;
    private PluginConfig currentConfig;

    public ExternalConfigManager(EndCrystalsPlugin plugin) {
        this.plugin = plugin;
        this.configDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        this.configPath = this.configDirectory.resolve("config.yml");
        this.legacyConfigPath = Path.of(System.getProperty("user.home"), "plugins", plugin.getName(), "config.yml")
                .toAbsolutePath()
                .normalize();
    }

    public void initialize() {
        try {
            Files.createDirectories(configDirectory);

            if (Files.notExists(configPath)) {
                if (Files.exists(legacyConfigPath)) {
                    Files.copy(legacyConfigPath, configPath);
                    plugin.getLogger().info("Copied legacy config from " + legacyConfigPath + " to " + configPath);
                } else {
                    copyEmbeddedConfig();
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize config at " + configPath, exception);
        }
    }

    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(configPath.toFile());

        try (InputStream resource = plugin.getResource("config.yml")) {
            if (resource != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(resource, StandardCharsets.UTF_8)
                );
                yaml.addDefaults(defaults);
                yaml.options().copyDefaults(true);
                normalizeConfig(defaults);
                yaml.save(configPath.toFile());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not merge default config values into " + configPath, exception);
        }

        this.currentConfig = PluginConfig.from(yaml);
    }

    public boolean toggle(String path, Boolean explicitValue) {
        ensureLoaded();

        if (!currentConfig.liveToggleKeys().contains(path)) {
            throw new IllegalArgumentException("Unsupported toggle: " + path);
        }

        Object rawValue = yaml.get(path);
        if (!(rawValue instanceof Boolean booleanValue)) {
            throw new IllegalArgumentException("The config path is not a boolean: " + path);
        }

        boolean newValue = explicitValue != null ? explicitValue : !booleanValue;
        yaml.set(path, newValue);

        try {
            yaml.save(configPath.toFile());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save config to " + configPath, exception);
        }

        reload();
        return newValue;
    }

    public PluginConfig currentConfig() {
        ensureLoaded();
        return currentConfig;
    }

    public Path configPath() {
        return configPath;
    }

    public Map<String, Boolean> liveToggleStates() {
        ensureLoaded();

        Map<String, Boolean> values = new LinkedHashMap<>();
        for (String key : currentConfig.liveToggleKeys()) {
            values.put(key, yaml.getBoolean(key));
        }

        return values;
    }

    public java.util.List<String> liveToggleKeys() {
        ensureLoaded();
        return currentConfig.liveToggleKeys();
    }

    private void ensureLoaded() {
        Objects.requireNonNull(currentConfig, "Config has not been loaded yet.");
        Objects.requireNonNull(yaml, "Config has not been loaded yet.");
    }

    private void copyEmbeddedConfig() throws IOException {
        try (InputStream resource = plugin.getResource("config.yml")) {
            if (resource == null) {
                throw new IllegalStateException("Embedded config.yml was not found.");
            }

            Files.copy(resource, configPath);
        }
    }

    private void normalizeConfig(YamlConfiguration defaults) {
        List<String> liveToggles = new ArrayList<>(yaml.getStringList("live-toggles"));
        for (String toggle : defaults.getStringList("live-toggles")) {
            if (!liveToggles.contains(toggle)) {
                liveToggles.add(toggle);
            }
        }
        yaml.set("live-toggles", liveToggles);

        normalizeProtectedEntityTypes(defaults);

        String legacyUsageSingular = "<yellow>Use <white>/_endcrystal debug</white>, <white>reload</white>, or <white>toggle &lt;setting&gt; [true|false]</white>.</yellow>";
        String legacyUsagePlural = "<yellow>Use <white>/_endcrystals debug</white>, <white>reload</white>, or <white>toggle &lt;setting&gt; [true|false]</white>.</yellow>";
        String legacyUsageBracket = "<yellow>Use <white>/_endcrystals debug</white>, <white>reload</white>, or <white>toggle [setting] [true|false]</white>.</yellow>";
        String defaultUsage = defaults.getString("messages.command-usage");
        String currentUsage = yaml.getString("messages.command-usage");
        if ((legacyUsageSingular.equals(currentUsage)
                || legacyUsagePlural.equals(currentUsage)
                || legacyUsageBracket.equals(currentUsage)) && defaultUsage != null) {
            yaml.set("messages.command-usage", defaultUsage);
        }
    }

    private void normalizeProtectedEntityTypes(YamlConfiguration defaults) {
        List<String> defaultTypes = defaults.getStringList("protection.protected-entity-types");
        List<String> currentTypes = yaml.getStringList("protection.protected-entity-types");

        if (currentTypes.isEmpty()) {
            yaml.set("protection.protected-entity-types", defaultTypes);
            return;
        }

        Set<String> currentTypeSet = toUpperCaseSet(currentTypes);
        Set<String> oldDefaultTypeSet = toUpperCaseSet(List.of(
                "ARMOR_STAND",
                "ITEM",
                "ITEM_FRAME",
                "GLOW_ITEM_FRAME",
                "PAINTING",
                "ITEM_DISPLAY",
                "BLOCK_DISPLAY",
                "TEXT_DISPLAY"
        ));

        if (currentTypeSet.equals(oldDefaultTypeSet)) {
            yaml.set("protection.protected-entity-types", defaultTypes);
        }
    }

    private Set<String> toUpperCaseSet(List<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(value.toUpperCase(java.util.Locale.ROOT));
        }
        return normalized;
    }
}
