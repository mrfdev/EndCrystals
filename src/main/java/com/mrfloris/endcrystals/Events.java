package com.mrfloris.endcrystals;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class Events
        implements Listener {
  public pluginEvents plugin;

  @EventHandler
  public void onEnity(EntityExplodeEvent event) {
    Entity entity = event.getEntity();
    if (entity instanceof org.bukkit.entity.EnderCrystal) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onEnityBoom(EntityDamageByEntityEvent event) {
    if (event.getEntity() instanceof org.bukkit.entity.EnderCrystal) {
      Entity attacker = event.getDamager();
      World.Environment environment = event.getEntity().getWorld().getEnvironment();
      if (environment != World.Environment.THE_END) {
        if (event.getDamager() instanceof Arrow) {
          Arrow arrow = (Arrow)event.getDamager();
          if (arrow.getShooter() instanceof Player) {
            Player player = (Player)arrow.getShooter();
            event.setCancelled(!player.hasPermission("pacocraft.break"));
          }
        }
        if (event.getDamager() instanceof Player) {
          event.setCancelled(!attacker.hasPermission("pacocraft.break"));
        }
      }
    }
  }
}