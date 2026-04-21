package com.mrfloris.endcrystals;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class EndCrystalsPlugin extends JavaPlugin {

    private final BuildMetadata buildMetadata = BuildMetadata.load();

    private ExternalConfigManager configManager;
    private PluginConfig config;

    @Override
    public void onEnable() {
        this.configManager = new ExternalConfigManager(this);

        try {
            reloadPluginConfig();
        } catch (IllegalStateException exception) {
            getLogger().log(Level.SEVERE, "Could not load the external config.", exception);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new CrystalProtectionListener(this), this);

        EndCrystalsCommand command = new EndCrystalsCommand(this);
        if (getCommand("_endcrystals") == null) {
            throw new IllegalStateException("The /_endcrystals command is missing from plugin.yml");
        }

        getCommand("_endcrystals").setExecutor(command);
        getCommand("_endcrystals").setTabCompleter(command);

        logRich("<green>Enabled.</green> <gray>Config:</gray> <white>%s</white>"
                .formatted(configManager.configPath()));
    }

    @Override
    public void onDisable() {
        if (config != null) {
            logRich("<yellow>Disabled.</yellow>");
        }
    }

    public void reloadPluginConfig() {
        configManager.initialize();
        configManager.reload();
        this.config = configManager.currentConfig();
    }

    public ExternalConfigManager configManager() {
        return configManager;
    }

    public PluginConfig config() {
        return config;
    }

    public BuildMetadata buildMetadata() {
        return buildMetadata;
    }

    public boolean hasBypassPermission(CommandSender sender) {
        return sender.hasPermission("1mb.endcrystals.bypass") || sender.hasPermission("1mb.endcrystals.admin");
    }

    public boolean hasDebugPermission(CommandSender sender) {
        return sender.hasPermission("1mb.endcrystals.debug") || sender.hasPermission("1mb.endcrystals.admin");
    }

    public boolean hasReloadPermission(CommandSender sender) {
        return sender.hasPermission("1mb.endcrystals.reload") || sender.hasPermission("1mb.endcrystals.admin");
    }

    public boolean hasTogglePermission(CommandSender sender) {
        return sender.hasPermission("1mb.endcrystals.toggle") || sender.hasPermission("1mb.endcrystals.admin");
    }

    public void sendConfiguredMessage(CommandSender sender, String key) {
        sendConfiguredMessage(sender, key, Collections.emptyMap());
    }

    public void sendConfiguredMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String message = config.message(key);
        sendRich(sender, applyPlaceholders(prefix() + message, placeholders), false);
    }

    public void sendRich(CommandSender sender, String message, boolean includePrefix) {
        String resolved = includePrefix ? prefix() + message : message;
        sender.sendRichMessage(resolved);
    }

    public void logRich(String message) {
        Bukkit.getConsoleSender().sendRichMessage(prefix() + message);
    }

    public String applyPlaceholders(String input, Map<String, String> placeholders) {
        String resolved = input;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        return resolved;
    }

    private String prefix() {
        return config != null ? config.prefix() : "<gray>[<gold>1MB-EndCrystals</gold>]</gray> ";
    }
}
