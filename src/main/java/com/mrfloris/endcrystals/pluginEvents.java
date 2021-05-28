package com.mrfloris.endcrystals;

import java.util.HashSet;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class pluginEvents
        extends JavaPlugin
{
    private Listener Listener = new Events();
    public final Logger logger = Logger.getLogger("Minecraft");
    public static pluginEvents plugin;

    public void onEnable() {
        PluginDescriptionFile pdffile = getDescription();
        this.logger.info(String.valueOf(pdffile.getName()) + " Version " + pdffile.getVersion() + " Has been Enabled!");
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this.Listener, (Plugin)this);
    }

    public void onDisable() {
        PluginDescriptionFile pdffile = getDescription();
        this.logger.info(String.valueOf(pdffile.getName()) + " Has been Disabled!");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        Player player = (Player)sender;
        if (commandLabel.equalsIgnoreCase("pacocraft") && player.hasPermission("pacocraft.spawn")) {
            Location location = player.getTargetBlock((HashSet)null, 20).getLocation().add(0.0D, 1.0D, 0.0D);
            Location location2 = location.clone().add(0.0D, 1.0D, 0.0D);
            Location location3 = player.getTargetBlock((HashSet)null, 20).getLocation().add(0.5D, 2.0D, 0.5D);
            if (location.getBlock().isEmpty() && location2.getBlock().isEmpty()) {
                location3.getWorld().spawnEntity(location3, EntityType.ENDER_CRYSTAL);
                location.getBlock().setType(Material.BEDROCK);
                location2.getBlock().setType(Material.FIRE);
                sender.sendMessage(ChatColor.BLACK + "[" + ChatColor.DARK_GREEN + "PacoCraft" + ChatColor.BLACK + "]" + ChatColor.RED + "Ender Crystal Spawned.");
            }

        } else if (commandLabel.equalsIgnoreCase("help") && player.hasPermission("pacocraft.help")) {
            player.sendMessage(ChatColor.BLACK + "[" + ChatColor.DARK_GREEN + "PacoCraft" + ChatColor.BLACK + "]");
            player.sendMessage(ChatColor.YELLOW + "Commands");
            player.sendMessage(ChatColor.YELLOW + "pacocraft");
            player.sendMessage("   Look at where you want your Ender Crystal to spawn");
        }
        return true;
    }
}
