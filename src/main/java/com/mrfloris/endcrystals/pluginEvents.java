package com.mrfloris.endcrystals;

import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class pluginEvents extends JavaPlugin {
    private final Listener Listener = new Events();
    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this.Listener, this);
    }
}