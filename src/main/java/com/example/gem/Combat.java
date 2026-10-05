package com.example.gem;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Combat {
    private Combat() {}

    private static boolean internal = false;
    private static final Set<UUID> stunned = new HashSet<>();

    /** True while ability damage is being dealt, so passives (crit lightning, stun...) ignore it. */
    public static boolean isInternal() { return internal; }
    public static boolean isStunned(Entity e) { return stunned.contains(e.getUniqueId()); }

    public static boolean isTarget(Player owner, Entity e) {
        if (!(e instanceof LivingEntity le) || e.equals(owner) || e instanceof ArmorStand) return false;
        if (e instanceof Player p && (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.CREATIVE)) return false;
        return !le.isDead();
    }

    public static List<LivingEntity> targets(Player owner, Location center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (isTarget(owner, e) && e.getLocation().distanceSquared(center) <= radius * radius) {
                out.add((LivingEntity) e);
            }
        }
        return out;
    }

    /** Deals damage in half-hearts (hp). 1 heart = 2 hp. Armor still applies. */
    public static void damage(LivingEntity target, Player source, double hp, boolean knockback) {
        if (target.isDead()) return;
        Vector vel = target.getVelocity();
        internal = true;
        try {
            target.setNoDamageTicks(0);
            target.damage(hp, source);
        } finally {
            internal = false;
        }
        if (!knockback) target.setVelocity(vel);
    }

    public static void push(LivingEntity t, Location from, double strength, double lift) {
        Vector v = t.getLocation().toVector().subtract(from.toVector());
        v.setY(0);
        if (v.lengthSquared() < 1e-4) v = from.getDirection().setY(0);
        if (v.lengthSquared() < 1e-4) v = new Vector(1, 0, 0);
        v.normalize().multiply(strength).setY(lift);
        t.setVelocity(v);
    }

    /** Burn damage that ignores fire resistance (it is normal damage, not fire damage). */
    public static void burn(Plugin plugin, LivingEntity t, Player src, double totalHp, int pulses, long interval) {
        t.setFireTicks((int) Math.max(t.getFireTicks(), pulses * interval));
        double each = totalHp / pulses;
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (!t.isValid() || t.isDead() || i++ >= pulses) { cancel(); return; }
                damage(t, src, each, false);
                t.getWorld().spawnParticle(Particle.FLAME, t.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.02);
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public static void stun(Plugin plugin, LivingEntity t, int ticks) {
        UUID id = t.getUniqueId();
        if (!stunned.add(id)) return;
        if (t instanceof Mob m) m.setAware(false);
        t.setFreezeTicks(t.getMaxFreezeTicks());
        t.getWorld().spawnParticle(Particle.SNOWFLAKE, t.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.02);
        new BukkitRunnable() {
            @Override public void run() {
                stunned.remove(id);
                if (t instanceof Mob m) m.setAware(true);
                t.setFreezeTicks(0);
            }
        }.runTaskLater(plugin, ticks);
    }
}
