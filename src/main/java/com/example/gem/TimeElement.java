package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;

public class TimeElement extends Element {
    private static final double ATTACK_RADIUS = 8;
    private static final double PASSIVE_RADIUS = 6;

    public TimeElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "time"; }
    public String displayName() { return "Time"; }
    public Color color() { return Color.fromRGB(110, 200, 255); }
    public BossBar.Color barColor() { return BossBar.Color.PINK; }
    public boolean legendary() { return true; }
    public String passiveText() { return "Regeneration II; under 4 hearts you slow everything around you"; }
    public String attackText() { return "Time field: everything but you is slowed (8s)"; }

    @Override public Map<PotionEffectType, Integer> effects() { return Map.of(PotionEffectType.REGENERATION, 1); }

    @Override
    public void tick(Player p, boolean active, int n) {
        if (active && p.getHealth() < 8.0) field(p, PASSIVE_RADIUS);
    }

    @Override
    public void attack(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.5f, 0.5f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead() || t >= 160) { cancel(); return; }
                field(p, ATTACK_RADIUS);
                t += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    /** Slows every living thing and every projectile (except yours) in range. Run every 5 ticks. */
    private void field(Player owner, double r) {
        Location c = owner.getLocation();
        World w = c.getWorld();
        for (Entity e : w.getNearbyEntities(c, r, r, r)) {
            if (e.equals(owner) || e.getLocation().distanceSquared(c) > r * r) continue;
            if (e instanceof Projectile pr) {
                if (!owner.equals(pr.getShooter())) pr.setVelocity(pr.getVelocity().multiply(0.4));
            } else if (e instanceof LivingEntity le && Combat.isTarget(owner, le)) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15, 3, true, false, false));
                le.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 15, 2, true, false, false));
            }
        }
        Fx.ring(c.clone().add(0, 0.2, 0), r, 48, color(), 1.3f);
        for (int i = 0; i < 12; i++) {
            Vector v = Vector.getRandom().subtract(new Vector(0.5, 0.5, 0.5));
            if (v.lengthSquared() < 1e-4) continue;
            v.normalize().multiply(r);
            if (v.getY() < 0) v.setY(-v.getY());
            Fx.dust(c.clone().add(v), color(), 1.0f, 1, 0);
        }
        Fx.orbit(c, color(), 1.0, 1.0);
    }
}
