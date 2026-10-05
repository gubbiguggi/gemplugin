package com.example.gem;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NullifierManager {
    public static final double RADIUS = 12;
    public static final long DURATION_MS = 30_000;
    public static final long COOLDOWN_MS = 60_000;

    private record Zone(Location center, double radius, long expires, UUID owner) {}

    private final GemPlugin plugin;
    private final List<Zone> zones = new ArrayList<>();

    public NullifierManager(GemPlugin plugin) { this.plugin = plugin; }

    public void create(Player owner) {
        Zone z = new Zone(owner.getLocation().clone(), RADIUS,
                System.currentTimeMillis() + DURATION_MS, owner.getUniqueId());
        zones.add(z);
        owner.getWorld().playSound(z.center(), Sound.BLOCK_BEACON_DEACTIVATE, 1.5f, 0.6f);
        Color grey = Color.fromRGB(200, 200, 215);
        new BukkitRunnable() {
            @Override public void run() {
                if (System.currentTimeMillis() >= z.expires()) { cancel(); return; }
                Location c = z.center();
                Fx.ring(c.clone().add(0, 0.2, 0), RADIUS, 80, grey, 1.2f);
                Fx.ring(c.clone().add(0, 2.5, 0), RADIUS, 80, grey, 0.9f);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /** Players inside someone else's zone lose their element. The zone's owner is immune. */
    public boolean isNullified(Player p) {
        long now = System.currentTimeMillis();
        for (Zone z : zones) {
            if (now >= z.expires() || z.owner().equals(p.getUniqueId())) continue;
            if (!z.center().getWorld().equals(p.getWorld())) continue;
            if (z.center().distanceSquared(p.getLocation()) <= z.radius() * z.radius()) return true;
        }
        return false;
    }

    public void prune() {
        long now = System.currentTimeMillis();
        zones.removeIf(z -> now >= z.expires());
    }
}
