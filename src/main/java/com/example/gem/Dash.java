package com.example.gem;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

/** A long lunge-style dash along the player's aim. */
public final class Dash {
    private Dash() {}

    public static void run(GemPlugin plugin, Player p, double speed, int ticks, Consumer<Player> perTick) {
        Vector dir = p.getLocation().getDirection().normalize();
        if (dir.getY() < 0.12) dir.setY(0.12);
        dir.normalize();
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead() || t++ >= ticks) { cancel(); return; }
                p.setVelocity(dir.clone().multiply(speed));
                p.setFallDistance(0);
                perTick.accept(p);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
