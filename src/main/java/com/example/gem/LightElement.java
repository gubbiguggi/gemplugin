package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Map;

public class LightElement extends Element {
    private static final int DURATION = 60;            // 3 seconds
    private static final int PULSE_EVERY = 4;          // ticks
    private static final double TOTAL_HP = 10;         // 5 hearts if the target stays in the beam the whole time
    private static final double PER_PULSE = TOTAL_HP / (DURATION / PULSE_EVERY);

    public LightElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "light"; }
    public String displayName() { return "Light"; }
    public Color color() { return Color.fromRGB(255, 245, 160); }
    public BossBar.Color barColor() { return BossBar.Color.YELLOW; }
    public boolean legendary() { return true; }
    public String passiveText() { return "Speed II, nearby players glow"; }
    public String attackText() { return "Aimed beam (5 hearts over time)"; }

    @Override public Map<PotionEffectType, Integer> effects() { return Map.of(PotionEffectType.SPEED, 1); }

    @Override
    public void tick(Player p, boolean active, int n) {
        if (!active) return;
        for (Entity e : p.getNearbyEntities(10, 10, 10)) {
            if (e instanceof Player o && !o.equals(p) && o.getGameMode() != GameMode.SPECTATOR
                    && o.getLocation().distanceSquared(p.getLocation()) <= 100) {
                o.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30, 0, true, false, false));
            }
        }
    }

    @Override
    public void attack(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.2f, 1.6f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead() || Combat.isStunned(p) || t >= DURATION) { cancel(); return; }
                Location eye = p.getEyeLocation();
                Vector dir = eye.getDirection();
                World w = eye.getWorld();
                double max = 30;
                RayTraceResult r = w.rayTrace(eye, dir, max, FluidCollisionMode.NEVER, true, 0.35,
                        en -> Combat.isTarget(p, en));
                double len = r != null ? r.getHitPosition().distance(eye.toVector()) : max;

                Location origin = eye.clone().subtract(0, 0.3, 0);
                for (double d = 1; d <= len; d += 0.4) {
                    Location l = origin.clone().add(dir.clone().multiply(d));
                    Fx.dust(l, color(), 1.3f, 1, 0.03);
                    if (((int) (d / 0.4)) % 3 == 0) w.spawnParticle(Particle.END_ROD, l, 1, 0, 0, 0, 0);
                }
                Fx.orbit(origin, color(), 0.8, 0);

                if (r != null) {
                    Location end = r.getHitPosition().toLocation(w);
                    w.spawnParticle(Particle.FLASH, end, 1);
                    if (t % PULSE_EVERY == 0 && r.getHitEntity() instanceof LivingEntity le) {
                        Combat.damage(le, p, PER_PULSE, false);
                    }
                }
                if (t % 10 == 0) w.playSound(eye, Sound.BLOCK_BEACON_AMBIENT, 0.8f, 2f);
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
