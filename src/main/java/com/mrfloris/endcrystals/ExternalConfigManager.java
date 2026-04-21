package com.mrfloris.endcrystals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ExternalConfigManager {

    private final EndCrystalsPlugin plugin;
    private final Path configDirectory;
    private final Path configPath;

    private YamlConfiguration yaml;
    private PluginConfig currentConfig;

    public ExternalConfigManager(EndCrystalsPlugin plugin) {
        this.plugin = plugin;
        this.configDirectory = Path.of(System.getProperty("user.home"), "plugins", plugin.getName());
        this.configPath = this.configDirectory.resolve("config.yml");
    }

    public void initialize() {
        try {
            Files.createDirectories(configDirectory);

            if (Files.notExists(configPath)) {
                try (InputStream resource = plugin.getResource("config.yml")) {
                    if (resource == null) {
                        throw new IllegalStateException("Embedded config.yml was not found.");
                    }

                    Files.copy(resource, configPath);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize config at " + configPath, exception);
        }
    }

    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(configPath.toFile());
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
}
