package com.mrfloris.endcrystals;

import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class CrystalProtectionListener implements Listener {

    private final EndCrystalsPlugin plugin;

    public CrystalProtectionListener(EndCrystalsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) {
            return;
        }

        PluginConfig config = plugin.config();
        if (!config.preventBlockDamage()) {
            return;
        }

        event.blockList().clear();
        if (config.clearExplosionYield()) {
            event.setYield(0.0F);
        }

        if (config.logBlockProtection()) {
            plugin.logRich("<gray>Prevented end crystal block damage at</gray> <white>%s</white>"
                    .formatted(formatLocation(event.getLocation().getWorld(), event.getLocation().getBlockX(), event.getLocation().getBlockY(), event.getLocation().getBlockZ())));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal)) {
            return;
        }

        Player attackingPlayer = resolvePlayer(event.getDamager());
        if (attackingPlayer == null) {
            return;
        }

        if (plugin.hasBypassPermission(attackingPlayer)) {
            return;
        }

        PluginConfig config = plugin.config();
        boolean inTheEnd = crystal.getWorld().getEnvironment() == World.Environment.THE_END;
        if (inTheEnd && config.allowPlayerBreakInTheEnd()) {
            return;
        }

        boolean playerAttack = event.getDamager() instanceof Player;
        if (playerAttack && !config.preventPlayerBreak()) {
            return;
        }

        boolean projectileAttack = event.getDamager() instanceof Projectile;
        if (projectileAttack && !config.preventProjectileBreak()) {
            return;
        }

        event.setCancelled(true);
        attackingPlayer.sendRichMessage(plugin.config().prefix() + plugin.config().message("player-break-blocked"));

        if (config.logCrystalBreaks()) {
            plugin.logRich("<gray>Blocked</gray> <white>%s</white> <gray>from breaking a protected end crystal at</gray> <white>%s</white>"
                    .formatted(
                            attackingPlayer.getName(),
                            formatLocation(crystal.getWorld(), crystal.getLocation().getBlockX(), crystal.getLocation().getBlockY(), crystal.getLocation().getBlockZ())
                    ));
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }

        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }

        return null;
    }

    private String formatLocation(World world, int x, int y, int z) {
        return world.getName() + " " + x + ", " + y + ", " + z;
    }
}
