package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WaterElement extends Element {
    public WaterElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "water"; }
    public String displayName() { return "Water"; }
    public Color color() { return Color.fromRGB(40, 120, 255); }
    public BossBar.Color barColor() { return BossBar.Color.BLUE; }
    public String passiveText() { return "Conduit power + dolphin's grace"; }
    public String attackText() { return "Dash with a damaging wave (3 hearts + knockback)"; }

    @Override public Map<PotionEffectType, Integer> effects() {
        return Map.of(PotionEffectType.CONDUIT_POWER, 0, PotionEffectType.DOLPHINS_GRACE, 0);
    }

    @Override
    public void attack(Player p) {
        Set<UUID> hit = new HashSet<>();
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.8f);
        Dash.run(plugin, p, 1.4, 10, pl -> {
            Location loc = pl.getLocation();
            World w = loc.getWorld();
            Vector dir = pl.getVelocity().clone().setY(0);
            if (dir.lengthSquared() < 0.01) dir = loc.getDirection().setY(0);
            if (dir.lengthSquared() < 0.01) dir = new Vector(1, 0, 0);
            dir.normalize();
            Vector right = new Vector(-dir.getZ(), 0, dir.getX());
            Location base = loc.clone().subtract(dir.clone().multiply(0.8));

            // the wave: an arc right behind you that fades into a trail
            for (double s = -1; s <= 1.01; s += 0.2) {
                double h = 0.3 + 1.0 * (1 - s * s);
                Location l = base.clone().add(right.clone().multiply(s * 2.2)).add(0, h, 0);
                Fx.dust(l, color(), 1.4f, 2, 0.1);
                w.spawnParticle(Particle.SPLASH, l, 3, 0.15, 0.15, 0.15, 0);
            }
            w.spawnParticle(Particle.BUBBLE_POP, loc.clone().add(0, 1, 0), 6, 0.5, 0.5, 0.5, 0.02);

            for (LivingEntity t : Combat.targets(pl, loc.clone().add(0, 0.5, 0), 2.6)) {
                if (!hit.add(t.getUniqueId())) continue;
                Combat.damage(t, pl, 6, true);
                Combat.push(t, loc, 1.1, 0.4);
            }
        });
    }
}
