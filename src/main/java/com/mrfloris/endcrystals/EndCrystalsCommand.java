package com.mrfloris.endcrystals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.permissions.Permission;

public final class EndCrystalsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("debug", "reload", "toggle");

    private final EndCrystalsPlugin plugin;

    public EndCrystalsCommand(EndCrystalsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        return switch (subcommand) {
            case "debug" -> handleDebug(sender);
            case "reload" -> handleReload(sender);
            case "toggle" -> handleToggle(sender, args);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && "toggle".equalsIgnoreCase(args[0])) {
            return filter(plugin.configManager().liveToggleKeys(), args[1]);
        }

        if (args.length == 3 && "toggle".equalsIgnoreCase(args[0])) {
            return filter(List.of("true", "false"), args[2]);
        }

        return Collections.emptyList();
    }

    private boolean handleDebug(CommandSender sender) {
        if (!plugin.hasDebugPermission(sender)) {
            plugin.sendConfiguredMessage(sender, "no-permission");
            return true;
        }

        PluginConfig config = plugin.config();
        Map<String, Boolean> liveToggles = plugin.configManager().liveToggleStates();

        plugin.sendRich(sender, "<gold><bold>1MB-EndCrystals Debug</bold></gold>", true);
        plugin.sendRich(sender, "<gray>Version:</gray> <white>%s</white> <gray>(build %s)</gray>"
                .formatted(plugin.getPluginMeta().getVersion(), plugin.buildMetadata().buildNumber()), false);
        plugin.sendRich(sender, "<gray>Compiled Against:</gray> <white>Java %s</white>, <white>Paper API %s</white>, <white>Paper %s</white>"
                .formatted(
                        plugin.buildMetadata().javaTarget(),
                        plugin.buildMetadata().paperApiVersion(),
                        plugin.buildMetadata().targetPaperVersion()
                ), false);
        plugin.sendRich(sender, "<gray>Declared API Floor:</gray> <white>%s</white> <gray>|</gray> <gray>MC Compatibility:</gray> <white>%s</white>"
                .formatted(
                        plugin.buildMetadata().declaredApiVersion(),
                        plugin.buildMetadata().targetMinecraftVersion()
                ), false);
        plugin.sendRich(sender, "<gray>Server:</gray> <white>%s</white> <gray>|</gray> <white>%s</white>"
                .formatted(Bukkit.getName(), Bukkit.getVersion()), false);
        plugin.sendRich(sender, "<gray>Bukkit:</gray> <white>%s</white> <gray>|</gray> <gray>Minecraft:</gray> <white>%s</white>"
                .formatted(Bukkit.getBukkitVersion(), Bukkit.getMinecraftVersion()), false);
        plugin.sendRich(sender, "<gray>Java:</gray> <white>%s</white>"
                .formatted(System.getProperty("java.version", "unknown")), false);
        plugin.sendRich(sender, "<gray>Config:</gray> <white>%s</white>"
                .formatted(plugin.configManager().configPath()), false);
        plugin.sendRich(sender, "<gray>Locale:</gray> <white>%s</white> <gray>|</gray> <white>%s</white>"
                .formatted(config.localeName(), plugin.configManager().localePath()), false);
        plugin.sendRich(sender, "<gray>Commands:</gray> <white>%s</white>"
                .formatted(plugin.commandSummary()), false);
        plugin.sendRich(sender, "<gray>Placeholders:</gray> <white>None</white>", false);
        plugin.sendRich(sender, "<gray>Permissions:</gray> <white>%s</white>"
                .formatted(formatPermissions(plugin.getPluginMeta().getPermissions())), false);
        plugin.sendRich(sender, "<gray>Protection:</gray> <white>prevent-block-damage=%s</white>, <white>prevent-player-break=%s</white>, <white>prevent-projectile-break=%s</white>"
                .formatted(
                        config.preventBlockDamage(),
                        config.preventPlayerBreak(),
                        config.preventProjectileBreak()
                ), false);
        plugin.sendRich(sender, "<gray>End Handling:</gray> <white>allow-player-break-in-the-end=%s</white>, <white>clear-explosion-yield=%s</white>"
                .formatted(
                        config.allowPlayerBreakInTheEnd(),
                        config.clearExplosionYield()
                ), false);
        plugin.sendRich(sender, "<gray>Entity Protection:</gray> <white>prevent-protected-entity-damage=%s</white>"
                .formatted(config.preventProtectedEntityDamage()), false);
        plugin.sendRich(sender, "<gray>Protected Types:</gray> <white>%s</white>"
                .formatted(config.protectedEntityTypesDisplay().isBlank() ? "None" : config.protectedEntityTypesDisplay()), false);
        plugin.sendRich(sender, "<gray>Live Toggles:</gray> <white>%s</white>"
                .formatted(formatLiveToggles(liveToggles)), false);
        plugin.sendRich(sender, "<gray>Hint:</gray> <white>/_endcrystals toggle [setting] [true|false]</white>", false);
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!plugin.hasReloadPermission(sender)) {
            plugin.sendConfiguredMessage(sender, "no-permission");
            return true;
        }

        try {
            plugin.reloadPluginConfig();
            plugin.sendConfiguredMessage(
                    sender,
                    "reloaded",
                    Map.of("path", plugin.configManager().configPath().toString())
            );
        } catch (IllegalStateException exception) {
            plugin.sendRich(sender, "<red>Reload failed:</red> <white>%s</white>".formatted(exception.getMessage()), true);
        }

        return true;
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!plugin.hasTogglePermission(sender)) {
            plugin.sendConfiguredMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 1 || "list".equalsIgnoreCase(args[1])) {
            plugin.sendRich(sender, "<gold>Live toggle keys:</gold> <white>%s</white>"
                    .formatted(String.join(", ", plugin.configManager().liveToggleKeys())), true);
            return true;
        }

        String setting = args[1];
        Boolean explicitValue = null;

        if (args.length >= 3) {
            if ("true".equalsIgnoreCase(args[2])) {
                explicitValue = true;
            } else if ("false".equalsIgnoreCase(args[2])) {
                explicitValue = false;
            } else {
                sendUsage(sender);
                return true;
            }
        }

        try {
            boolean newValue = plugin.configManager().toggle(setting, explicitValue);
            plugin.sendConfiguredMessage(
                    sender,
                    "toggle-updated",
                    Map.of("setting", setting, "value", String.valueOf(newValue))
            );
        } catch (IllegalArgumentException exception) {
            plugin.sendConfiguredMessage(
                    sender,
                    "unknown-setting",
                    Map.of("setting", setting)
            );
        } catch (IllegalStateException exception) {
            plugin.sendRich(sender, "<red>Toggle failed:</red> <white>%s</white>".formatted(exception.getMessage()), true);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        plugin.sendConfiguredMessage(sender, "command-usage");
    }

    private List<String> filter(List<String> values, String token) {
        String lowerToken = token.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lowerToken))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String formatPermissions(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "None";
        }

        return permissions.stream()
                .map(Permission::getName)
                .collect(Collectors.joining(", "));
    }

    private String formatLiveToggles(Map<String, Boolean> liveToggles) {
        if (liveToggles.isEmpty()) {
            return "None";
        }

        return liveToggles.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }
}
