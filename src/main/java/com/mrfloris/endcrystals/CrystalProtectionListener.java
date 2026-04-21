package com.mrfloris.endcrystals;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class CrystalProtectionListener implements Listener {

    private static final long EXPLOSION_TRACK_WINDOW_MILLIS = 2_000L;
    private static final double EXPLOSION_TRACK_RADIUS_SQUARED = 100.0D;

    private final EndCrystalsPlugin plugin;
    private final Deque<TrackedExplosion> recentExplosions = new ArrayDeque<>();

    public CrystalProtectionListener(EndCrystalsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCrystalPrime(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof EnderCrystal crystal) {
            trackExplosion(crystal.getWorld().getUID(), crystal.getLocation().getX(), crystal.getLocation().getY(), crystal.getLocation().getZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrystalExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal)) {
            return;
        }

        trackExplosion(
                event.getLocation().getWorld().getUID(),
                event.getLocation().getX(),
                event.getLocation().getY(),
                event.getLocation().getZ()
        );

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
    public void onProtectedEntityDamaged(EntityDamageEvent event) {
        PluginConfig config = plugin.config();
        if (!config.preventProtectedEntityDamage()) {
            return;
        }

        if (!config.protectsEntity(event.getEntity())) {
            return;
        }

        if (!isEndCrystalExplosion(event)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProtectedVehicleDamaged(VehicleDamageEvent event) {
        PluginConfig config = plugin.config();
        if (!config.preventProtectedEntityDamage()) {
            return;
        }

        if (!config.protectsEntity(event.getVehicle())) {
            return;
        }

        if (!isEndCrystalExplosion(event.getVehicle().getLocation(), event.getAttacker())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProtectedVehicleDestroyed(VehicleDestroyEvent event) {
        PluginConfig config = plugin.config();
        if (!config.preventProtectedEntityDamage()) {
            return;
        }

        if (!config.protectsEntity(event.getVehicle())) {
            return;
        }

        if (!isEndCrystalExplosion(event.getVehicle().getLocation(), event.getAttacker())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        PluginConfig config = plugin.config();
        if (!config.preventProtectedEntityDamage()) {
            return;
        }

        if (!config.protectsEntity(event.getEntity())) {
            return;
        }

        if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) {
            return;
        }

        if (!isNearTrackedCrystalExplosion(event.getEntity().getLocation())) {
            return;
        }

        event.setCancelled(true);
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

    private boolean isEndCrystalExplosion(EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return false;
        }

        Entity causingEntity = event.getDamageSource().getCausingEntity();
        if (causingEntity instanceof EnderCrystal) {
            return true;
        }

        Entity directEntity = event.getDamageSource().getDirectEntity();
        if (directEntity instanceof EnderCrystal) {
            return true;
        }

        return isEndCrystalExplosion(event.getEntity().getLocation(), causingEntity)
                || isEndCrystalExplosion(event.getEntity().getLocation(), directEntity);
    }

    private boolean isEndCrystalExplosion(org.bukkit.Location location, Entity causingEntity) {
        if (causingEntity instanceof EnderCrystal) {
            return true;
        }

        return isNearTrackedCrystalExplosion(location);
    }

    private void trackExplosion(UUID worldId, double x, double y, double z) {
        purgeExpiredExplosions();
        recentExplosions.addLast(new TrackedExplosion(
                worldId,
                x,
                y,
                z,
                System.currentTimeMillis() + EXPLOSION_TRACK_WINDOW_MILLIS
        ));
    }

    private boolean isNearTrackedCrystalExplosion(org.bukkit.Location location) {
        purgeExpiredExplosions();

        for (TrackedExplosion explosion : recentExplosions) {
            if (!explosion.worldId().equals(location.getWorld().getUID())) {
                continue;
            }

            double dx = explosion.x() - location.getX();
            double dy = explosion.y() - location.getY();
            double dz = explosion.z() - location.getZ();
            double distanceSquared = (dx * dx) + (dy * dy) + (dz * dz);
            if (distanceSquared <= EXPLOSION_TRACK_RADIUS_SQUARED) {
                return true;
            }
        }

        return false;
    }

    private void purgeExpiredExplosions() {
        long now = System.currentTimeMillis();
        Iterator<TrackedExplosion> iterator = recentExplosions.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAtMillis() < now) {
                iterator.remove();
            }
        }
    }

    private record TrackedExplosion(UUID worldId, double x, double y, double z, long expiresAtMillis) {
    }
}
