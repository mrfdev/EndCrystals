package com.mrfloris.endcrystals;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;

final class ConfiguredAliasCommand extends Command {

    private final PluginCommand delegate;

    ConfiguredAliasCommand(String alias, PluginCommand delegate) {
        super(alias, delegate.getDescription(), delegate.getUsage(), List.of());
        this.delegate = delegate;
        setPermission(delegate.getPermission());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return delegate.execute(sender, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return delegate.tabComplete(sender, alias, args);
    }
}
