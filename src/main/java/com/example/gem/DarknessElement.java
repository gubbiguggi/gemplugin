package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Map;

public class DarknessElement extends Element {
    private static final int DURATION = 120;           // 6 seconds
    private static final double PULL_RADIUS = 8;
    private static final double DAMAGE_RADIUS = 3.5;
    private static final double PER_PULSE = 8.0 / (DURATION / 10); // 4 hearts total, pulse every 10 ticks

    public DarknessElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "darkness"; }
    public String displayName() { return "Darkness"; }
    public Color color() { return Color.fromRGB(110, 30, 170); }
    public BossBar.Color barColor() { return BossBar.Color.PURPLE; }
    public boolean legendary() { return true; }
    public String passiveText() { return "Permanent Strength II"; }
    public String attackText() { return "Dark block that pulls and drains (4 hearts over time)"; }

    @Override public Map<PotionEffectType, Integer> effects() { return Map.of(PotionEffectType.STRENGTH, 1); }

    @Override
    public void attack(Player p) {
        World w = p.getWorld();
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        RayTraceResult r = w.rayTraceBlocks(eye, dir, 20, FluidCollisionMode.NEVER, true);
        Location pos;
        if (r != null) {
            pos = r.getHitPosition().toLocation(w);
            if (r.getHitBlockFace() != null) pos.add(r.getHitBlockFace().getDirection());
        } else {
            pos = eye.clone().add(dir.clone().multiply(20));
        }
        final Location center = pos;

        BlockDisplay core = w.spawn(center, BlockDisplay.class, d -> {
            d.setBlock(Material.BLACK_CONCRETE.createBlockData());
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(-0.6f, -0.6f, -0.6f), new AxisAngle4f(),
                    new Vector3f(1.2f), new AxisAngle4f()));
        });
        w.playSound(center, Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.2f, 0.6f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= DURATION || !core.isValid()) {
                    w.spawnParticle(Particle.EXPLOSION, center, 1);
                    Fx.dust(center, color(), 2f, 40, 1.0);
                    core.remove();
                    cancel();
                    return;
                }
                double a = t * 0.35;
                double rad = 3.0 - (t % 20) * 0.13;
                for (int i = 0; i < 4; i++) {
                    double ang = a + i * Math.PI / 2;
                    Location l = center.clone().add(Math.cos(ang) * rad, Math.sin(ang * 2) * 0.6, Math.sin(ang) * rad);
                    Fx.dust(l, color(), 1.4f, 1, 0);
                }
                w.spawnParticle(Particle.SQUID_INK, center, 2, 0.4, 0.4, 0.4, 0.02);
                w.spawnParticle(Particle.REVERSE_PORTAL, center, 6, 0.3, 0.3, 0.3, 0.05);

                for (LivingEntity e : Combat.targets(p, center, PULL_RADIUS)) {
                    Vector pull = center.toVector().subtract(e.getBoundingBox().getCenter());
                    double dist = pull.length();
                    if (dist > 0.6) e.setVelocity(e.getVelocity().multiply(0.7).add(pull.normalize().multiply(0.28)));
                    if (t % 10 == 0 && dist <= DAMAGE_RADIUS) Combat.damage(e, p, PER_PULSE, false);
                }
                t++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
