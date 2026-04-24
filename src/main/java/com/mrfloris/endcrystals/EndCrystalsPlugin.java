package com.mrfloris.endcrystals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EndCrystalsPlugin extends JavaPlugin {

    private static final String PRIMARY_COMMAND = "_endcrystals";

    private final BuildMetadata buildMetadata = BuildMetadata.load();

    private ExternalConfigManager configManager;
    private PluginConfig config;
    private PluginCommand rootCommand;
    private final Map<String, Command> aliasCommands = new LinkedHashMap<>();
    private List<String> registeredCommandAliases = List.of();

    @Override
    public void onEnable() {
        this.configManager = new ExternalConfigManager(this);
        this.rootCommand = requireRootCommand();

        EndCrystalsCommand command = new EndCrystalsCommand(this);
        rootCommand.setExecutor(command);
        rootCommand.setTabCompleter(command);

        try {
            reloadPluginConfig();
        } catch (IllegalStateException exception) {
            getLogger().log(Level.SEVERE, "Could not load the external config.", exception);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new CrystalProtectionListener(this), this);

        logRich("<green>Enabled.</green> <gray>Config:</gray> <white>%s</white>"
                .formatted(configManager.configPath()));
    }

    @Override
    public void onDisable() {
        if (rootCommand != null) {
            unregisterAliasCommands(getServer().getCommandMap());
        }

        if (config != null) {
            logRich("<yellow>Disabled.</yellow>");
        }
    }

    public void reloadPluginConfig() {
        configManager.initialize();
        configManager.reload();
        this.config = configManager.currentConfig();
        syncCommandAliases();
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

    public String commandSummary() {
        String activeAliases = registeredCommandAliasesDisplay();
        if (config == null) {
            return "/" + PRIMARY_COMMAND + " aliases=[" + activeAliases + "]";
        }

        String configuredAliases = config.commandAliasesDisplay();
        if (configuredAliases.equals(activeAliases)) {
            return "/" + PRIMARY_COMMAND + " aliases=[" + activeAliases + "]";
        }

        return "/" + PRIMARY_COMMAND + " aliases=[" + activeAliases + "] configured=[" + configuredAliases + "]";
    }

    public boolean hasBypassPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission("onembendcrystals.break");
    }

    public boolean hasAdminPermission(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return true;
        }

        return sender.hasPermission("onembendcrystals.admin");
    }

    public boolean hasDebugPermission(CommandSender sender) {
        return hasAdminPermission(sender) || sender.hasPermission("onembendcrystals.debug");
    }

    public boolean hasReloadPermission(CommandSender sender) {
        return hasAdminPermission(sender) || sender.hasPermission("onembendcrystals.reload");
    }

    public boolean hasTogglePermission(CommandSender sender) {
        return hasAdminPermission(sender) || sender.hasPermission("onembendcrystals.toggle");
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

    private PluginCommand requireRootCommand() {
        PluginCommand command = getCommand(PRIMARY_COMMAND);
        if (command == null) {
            throw new IllegalStateException("The /" + PRIMARY_COMMAND + " command is missing from plugin.yml");
        }

        return command;
    }

    private void syncCommandAliases() {
        CommandMap commandMap = getServer().getCommandMap();
        unregisterAliasCommands(commandMap);

        java.util.List<String> activeAliases = new java.util.ArrayList<>();
        Set<String> unavailableAliases = new LinkedHashSet<>();
        String fallbackPrefix = getPluginMeta().getName().toLowerCase(java.util.Locale.ROOT);

        for (String alias : config.commandAliases()) {
            Command existing = commandMap.getCommand(alias);
            if (existing != null && existing != rootCommand) {
                unavailableAliases.add(alias);
                continue;
            }

            ConfiguredAliasCommand aliasCommand = new ConfiguredAliasCommand(alias, rootCommand);
            commandMap.register(alias, fallbackPrefix, aliasCommand);

            if (commandMap.getCommand(alias) == aliasCommand) {
                aliasCommands.put(alias, aliasCommand);
                activeAliases.add(alias);
            } else {
                removeRegisteredEntries(commandMap, aliasCommand);
                aliasCommand.unregister(commandMap);
                unavailableAliases.add(alias);
            }
        }

        this.registeredCommandAliases = List.copyOf(activeAliases);
        warnAboutUnavailableAliases(unavailableAliases);
        refreshOnlineCommandTrees();
    }

    private void unregisterAliasCommands(CommandMap commandMap) {
        for (Command aliasCommand : aliasCommands.values()) {
            removeRegisteredEntries(commandMap, aliasCommand);
            aliasCommand.unregister(commandMap);
        }

        aliasCommands.clear();
        registeredCommandAliases = List.of();
    }

    private void removeRegisteredEntries(CommandMap commandMap, Command command) {
        List<String> keysToRemove = commandMap.getKnownCommands().entrySet().stream()
                .filter(entry -> entry.getValue() == command)
                .map(Map.Entry::getKey)
                .toList();

        for (String key : keysToRemove) {
            commandMap.getKnownCommands().remove(key);
        }
    }

    private void warnAboutUnavailableAliases(Set<String> unavailableAliases) {
        if (!unavailableAliases.isEmpty()) {
            getLogger().warning("Some configured command aliases could not be registered: "
                    + String.join(", ", unavailableAliases));
        }
    }

    private void refreshOnlineCommandTrees() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    private String registeredCommandAliasesDisplay() {
        return registeredCommandAliases.isEmpty() ? "None" : String.join(", ", registeredCommandAliases);
    }
}
