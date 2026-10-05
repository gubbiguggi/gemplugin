package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class LightningElement extends Element {
    private static final double RADIUS = 10;

    public LightningElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "lightning"; }
    public String displayName() { return "Lightning"; }
    public Color color() { return Color.fromRGB(255, 235, 70); }
    public BossBar.Color barColor() { return BossBar.Color.YELLOW; }
    public String passiveText() { return "30% chance for crits to summon lightning (+1 heart)"; }
    public String attackText() { return "Storm: lightning on all nearby enemies (3 hearts + blindness)"; }

    @Override
    public void onDealDamage(Player p, EntityDamageByEntityEvent e) {
        if (!e.isCritical() || !(e.getEntity() instanceof LivingEntity t)) return;
        if (ThreadLocalRandom.current().nextDouble() >= 0.30) return;
        t.getWorld().strikeLightningEffect(t.getLocation()); // visual only, no fire
        e.setDamage(e.getDamage() + 2); // +1 heart on top of the crit
        Fx.dust(t.getLocation().add(0, 1, 0), color(), 1.5f, 20, 0.5);
    }

    @Override
    public void attack(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.5f);
        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (!p.isOnline() || p.isDead()) { cancel(); return; }
                Location c = p.getLocation();
                World w = c.getWorld();
                Location cloud = c.clone().add(0, 7, 0);
                Fx.ring(cloud, RADIUS * 0.8, 40, Color.fromRGB(70, 70, 90), 2.0f);
                w.spawnParticle(Particle.CLOUD, cloud, 15, RADIUS * 0.5, 0.4, RADIUS * 0.5, 0.01);
                Fx.orbit(c, color(), 1.2, 0.5);

                if (t == 15) {
                    for (LivingEntity target : Combat.targets(p, c, RADIUS)) {
                        Location l = target.getLocation();
                        w.strikeLightningEffect(l);
                        Fx.dust(l.clone().add(0, 1, 0), color(), 1.6f, 25, 0.6);
                        Combat.damage(target, p, 6, true);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    }
                }
                if (++t > 30) cancel();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
