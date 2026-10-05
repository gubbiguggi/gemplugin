package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;

public class FireElement extends Element {
    private static final double MAX_HP = 6.0;        // 3 hearts on a perfect hit
    private static final double SPLASH_RADIUS = 2.5;
    private static final double BURN_HP = 4.0;       // 2 hearts total, ignores fire resistance

    public FireElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "fire"; }
    public String displayName() { return "Fire"; }
    public Color color() { return Color.fromRGB(255, 110, 20); }
    public BossBar.Color barColor() { return BossBar.Color.RED; }
    public String passiveText() { return "Permanent fire resistance"; }
    public String attackText() { return "Fire charge (3 hearts + burn)"; }

    @Override public Map<PotionEffectType, Integer> effects() { return Map.of(PotionEffectType.FIRE_RESISTANCE, 0); }

    @Override
    public void attack(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        Location start = eye.clone().add(dir.clone().multiply(0.8));
        p.getWorld().playSound(eye, Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
        Bolt.launch(plugin, p, start, dir, new ItemStack(Material.FIRE_CHARGE), 0.7f, 1.2, 50, 0.45,
                loc -> {
                    Fx.dust(loc, color(), 1.3f, 4, 0.12);
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.08, 0.08, 0.08, 0.01);
                    Fx.orbit(loc, color(), 0.5, 0);
                },
                (at, direct) -> explode(p, at, direct));
    }

    private void explode(Player owner, Location at, LivingEntity direct) {
        World w = at.getWorld();
        w.spawnParticle(Particle.EXPLOSION, at, 1);
        w.spawnParticle(Particle.FLAME, at, 40, 0.5, 0.5, 0.5, 0.08);
        Fx.dust(at, color(), 1.6f, 30, 0.7);
        w.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.5f); // visual only: no real explosion

        if (direct != null) hurt(owner, direct, MAX_HP);
        for (LivingEntity t : Combat.targets(owner, at, SPLASH_RADIUS)) {
            if (t.equals(direct)) continue;
            double dist = t.getBoundingBox().getCenter().distance(at.toVector());
            double dmg = MAX_HP * Math.max(0, 1 - dist / SPLASH_RADIUS);
            if (dmg > 0.5) hurt(owner, t, dmg);
        }
    }

    private void hurt(Player owner, LivingEntity t, double hp) {
        Combat.damage(t, owner, hp, true);
        Combat.burn(plugin, t, owner, BURN_HP, 4, 10);
    }
}
