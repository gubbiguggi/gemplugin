package com.example.gem;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/** Particle helpers. Everything uses colour-shifting dust so each element has its own look. */
public final class Fx {
    private Fx() {}

    public static void dust(Location l, Color c, float size, int count, double spread) {
        Color to = c.mixColors(Color.WHITE);
        l.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, l, count, spread, spread, spread, 0,
                new Particle.DustTransition(c, to, size), true);
    }

    public static void ring(Location center, double radius, int points, Color c, float size) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            dust(center.clone().add(Math.cos(a) * radius, 0, Math.sin(a) * radius), c, size, 1, 0);
        }
    }

    /** Three motes spiralling around a point. Call every tick for a smooth swirl. */
    public static void orbit(Location center, Color c, double radius, double height) {
        double base = Bukkit.getCurrentTick() * 0.45;
        for (int i = 0; i < 3; i++) {
            double a = base + i * (2 * Math.PI / 3);
            Location l = center.clone().add(Math.cos(a) * radius,
                    height + Math.sin(base * 0.5 + i) * 0.4, Math.sin(a) * radius);
            dust(l, c, 1.0f, 1, 0);
        }
    }
}
