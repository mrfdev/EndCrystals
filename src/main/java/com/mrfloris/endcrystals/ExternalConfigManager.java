package com.mrfloris.endcrystals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ExternalConfigManager {

    private static final String DEFAULT_LOCALE_FILE = "Locale_EN.yml";

    private final EndCrystalsPlugin plugin;
    private final Path configDirectory;
    private final Path configPath;
    private final Path translationsDirectory;

    private YamlConfiguration yaml;
    private YamlConfiguration configDefaults;
    private YamlConfiguration localeYaml;
    private PluginConfig currentConfig;
    private Path localePath;

    public ExternalConfigManager(EndCrystalsPlugin plugin) {
        this.plugin = plugin;
        this.configDirectory = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        this.configPath = this.configDirectory.resolve("config.yml");
        this.translationsDirectory = this.configDirectory.resolve("Translations");
    }

    public void initialize() {
        try {
            Files.createDirectories(configDirectory);
            Files.createDirectories(translationsDirectory);

            if (Files.notExists(configPath)) {
                copyEmbeddedConfig();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize config at " + configPath, exception);
        }
    }

    public void reload() {
        this.yaml = loadYaml(configPath);

        try (InputStream resource = plugin.getResource("config.yml")) {
            if (resource != null) {
                this.configDefaults = loadYaml(resource, "config.yml");
                yaml.addDefaults(configDefaults);
                yaml.options().copyDefaults(true);
                normalizeConfig(configDefaults);
                applyCommentsFromDefaults(yaml, configDefaults);
                saveYaml(yaml, configPath, "config");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not merge default config values into " + configPath, exception);
        }

        String resolvedLocaleFile = normalizeLocaleFileName(yaml.getString("translations.locale", DEFAULT_LOCALE_FILE));
        this.localePath = translationsDirectory.resolve(resolvedLocaleFile);
        initializeLocaleFile(resolvedLocaleFile);
        this.localeYaml = loadYaml(localePath);

        try {
            YamlConfiguration localeDefaults = loadEmbeddedLocaleDefaults(resolvedLocaleFile);
            localeYaml.addDefaults(localeDefaults);
            localeYaml.options().copyDefaults(true);
            normalizeLocaleConfig(localeDefaults);
            applyCommentsFromDefaults(localeYaml, localeDefaults);
            saveYaml(localeYaml, localePath, "locale");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not merge default locale values into " + localePath, exception);
        }

        this.currentConfig = PluginConfig.from(yaml, localeYaml, resolvedLocaleFile);
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
            if (configDefaults != null) {
                applyCommentsFromDefaults(yaml, configDefaults);
            }
            saveYaml(yaml, configPath, "config");
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

    public Path localePath() {
        ensureLoaded();
        return localePath;
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
        Objects.requireNonNull(localeYaml, "Locale has not been loaded yet.");
        Objects.requireNonNull(localePath, "Locale has not been loaded yet.");
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

    private void initializeLocaleFile(String localeFileName) {
        try {
            if (Files.exists(localePath)) {
                return;
            }

            YamlConfiguration localeDefaults = loadEmbeddedLocaleDefaults(localeFileName);

            if (yaml.isConfigurationSection("messages")) {
                for (String key : yaml.getConfigurationSection("messages").getKeys(true)) {
                    localeDefaults.set("messages." + key, yaml.get("messages." + key));
                }
            }

            saveYaml(localeDefaults, localePath, "locale");
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize locale at " + localePath, exception);
        }
    }

    private YamlConfiguration loadEmbeddedLocaleDefaults(String localeFileName) throws IOException {
        String resourcePath = resolveEmbeddedLocaleResource(localeFileName);
        try (InputStream resource = plugin.getResource(resourcePath)) {
            if (resource == null) {
                throw new IllegalStateException("Embedded locale resource was not found: " + resourcePath);
            }

            return loadYaml(resource, resourcePath);
        }
    }

    private String resolveEmbeddedLocaleResource(String localeFileName) {
        String requested = "Translations/" + localeFileName;
        if (plugin.getResource(requested) != null) {
            return requested;
        }

        if (!DEFAULT_LOCALE_FILE.equals(localeFileName)) {
            plugin.getLogger().warning("Locale " + localeFileName + " was not bundled; falling back to " + DEFAULT_LOCALE_FILE);
        }

        return "Translations/" + DEFAULT_LOCALE_FILE;
    }

    private String normalizeLocaleFileName(String configuredLocale) {
        String normalized = configuredLocale == null ? "" : configuredLocale.trim();
        if (normalized.isBlank()) {
            normalized = DEFAULT_LOCALE_FILE;
        }

        if (!normalized.toLowerCase(java.util.Locale.ROOT).endsWith(".yml")) {
            normalized = normalized + ".yml";
        }

        return Path.of(normalized).getFileName().toString();
    }

    private void normalizeLocaleConfig(YamlConfiguration defaults) {
        String legacyUsageSingular = "<yellow>Use <white>/_endcrystal debug</white>, <white>reload</white>, or <white>toggle &lt;setting&gt; [true|false]</white>.</yellow>";
        String legacyUsagePlural = "<yellow>Use <white>/_endcrystals debug</white>, <white>reload</white>, or <white>toggle &lt;setting&gt; [true|false]</white>.</yellow>";
        String legacyUsageBracket = "<yellow>Use <white>/_endcrystals debug</white>, <white>reload</white>, or <white>toggle [setting] [true|false]</white>.</yellow>";
        String defaultUsage = defaults.getString("messages.command-usage");
        String currentUsage = localeYaml.getString("messages.command-usage");
        if ((legacyUsageSingular.equals(currentUsage)
                || legacyUsagePlural.equals(currentUsage)
                || legacyUsageBracket.equals(currentUsage)) && defaultUsage != null) {
            localeYaml.set("messages.command-usage", defaultUsage);
        }
    }

    private void applyCommentsFromDefaults(YamlConfiguration target, YamlConfiguration defaults) {
        target.options().parseComments(true);
        target.options().setHeader(mergeCommentLists(target.options().getHeader(), defaults.options().getHeader()));

        applySectionComments(target, defaults, defaults);
    }

    private void applySectionComments(YamlConfiguration target, ConfigurationSection defaultsRoot, ConfigurationSection currentSection) {
        for (String key : currentSection.getKeys(false)) {
            String path = currentSection == defaultsRoot ? key : currentSection.getCurrentPath() + "." + key;
            applyPathComments(target, defaultsRoot, path);

            ConfigurationSection child = currentSection.getConfigurationSection(key);
            if (child != null) {
                applySectionComments(target, defaultsRoot, child);
            }
        }
    }

    private void applyPathComments(YamlConfiguration target, ConfigurationSection defaultsRoot, String path) {
        List<String> mergedComments = mergeCommentLists(target.getComments(path), defaultsRoot.getComments(path));
        if (!mergedComments.equals(target.getComments(path))) {
            target.setComments(path, mergedComments);
        }

        List<String> mergedInlineComments = mergeCommentLists(target.getInlineComments(path), defaultsRoot.getInlineComments(path));
        if (!mergedInlineComments.equals(target.getInlineComments(path))) {
            target.setInlineComments(path, mergedInlineComments);
        }
    }

    private List<String> mergeCommentLists(List<String> existing, List<String> defaults) {
        List<String> safeExisting = sanitizeCommentLines(existing);
        List<String> safeDefaults = sanitizeCommentLines(defaults);

        if (safeExisting.isEmpty() && safeDefaults.isEmpty()) {
            return List.of();
        }

        if (safeExisting.isEmpty()) {
            return List.copyOf(safeDefaults);
        }

        if (safeDefaults.isEmpty() || safeExisting.equals(safeDefaults)) {
            return List.copyOf(safeExisting);
        }

        List<String> merged = new ArrayList<>();
        addUniqueComments(merged, safeDefaults);
        addUniqueComments(merged, safeExisting);
        return List.copyOf(merged);
    }

    private List<String> sanitizeCommentLines(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }

        List<String> sanitized = new ArrayList<>(comments.size());
        for (String comment : comments) {
            if (comment == null || comment.isBlank()) {
                continue;
            }
            sanitized.add(comment);
        }

        if (sanitized.isEmpty()) {
            return List.of();
        }
        return List.copyOf(sanitized);
    }

    private void addUniqueComments(List<String> target, List<String> source) {
        for (String line : source) {
            if (!target.contains(line)) {
                target.add(line);
            }
        }
    }

    private void saveYaml(YamlConfiguration configuration, Path path, String label) throws IOException {
        Files.createDirectories(path.getParent());
        try {
            configuration.save(path.toFile());
        } catch (IOException exception) {
            throw new IOException("Could not save " + label + " YAML to " + path, exception);
        }
    }

    private YamlConfiguration loadYaml(Path path) {
        YamlConfiguration configuration = newYamlConfiguration();
        if (Files.notExists(path)) {
            return configuration;
        }

        try {
            configuration.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load YAML from " + path, exception);
        }
    }

    private YamlConfiguration loadYaml(InputStream stream, String sourceName) {
        YamlConfiguration configuration = newYamlConfiguration();

        try {
            configuration.loadFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Could not load YAML from " + sourceName, exception);
        }
    }

    private YamlConfiguration newYamlConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.options().parseComments(true);
        return configuration;
    }
}
