package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class IceElement extends Element {
    public IceElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "ice"; }
    public String displayName() { return "Ice"; }
    public Color color() { return Color.fromRGB(140, 220, 255); }
    public BossBar.Color barColor() { return BossBar.Color.BLUE; }
    public String passiveText() { return "10% chance to stun on hit (0.5s)"; }
    public String attackText() { return "Two icicles (1.5 hearts each)"; }

    @Override
    public void onDealDamage(Player p, EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity t)) return;
        if (ThreadLocalRandom.current().nextDouble() < 0.10) Combat.stun(plugin, t, 10);
    }

    @Override
    public void attack(Player p) {
        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection();
        Vector right = new Vector(-dir.getZ(), 0, dir.getX());
        if (right.lengthSquared() < 1e-4) right = new Vector(1, 0, 0);
        right.normalize();
        p.getWorld().playSound(eye, Sound.BLOCK_GLASS_BREAK, 1f, 1.6f);

        for (int side = -1; side <= 1; side += 2) {
            Vector d = dir.clone().rotateAroundY(Math.toRadians(3 * side));
            Location start = eye.clone().add(dir.clone().multiply(0.8)).add(right.clone().multiply(0.35 * side)).subtract(0, 0.2, 0);
            ItemStack icicle = plugin.modelItem("icicle");
            Bolt.launch(plugin, p, start, d, icicle, 0.9f, 1.6, 45, 0.35,
                    loc -> {
                        Fx.dust(loc, color(), 1.1f, 2, 0.05);
                        loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 2, 0.05, 0.05, 0.05, 0.01);
                    },
                    (at, target) -> {
                        at.getWorld().spawnParticle(Particle.SNOWFLAKE, at, 20, 0.3, 0.3, 0.3, 0.05);
                        at.getWorld().spawnParticle(Particle.BLOCK, at, 15, 0.2, 0.2, 0.2, Material.ICE.createBlockData());
                        at.getWorld().playSound(at, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.2f);
                        if (target != null) Combat.damage(target, p, 3, true);
                    });
        }
    }
}
