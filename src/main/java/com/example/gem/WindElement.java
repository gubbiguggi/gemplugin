package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class WindElement extends Element {
    private static final double CAP_HP = 12;       // 6 hearts
    private static final double MACE_CAP_HP = 18;  // 9 hearts

    public WindElement(GemPlugin plugin) { super(plugin); }

    public String id() { return "wind"; }
    public String displayName() { return "Wind"; }
    public Color color() { return Color.fromRGB(200, 240, 235); }
    public BossBar.Color barColor() { return BossBar.Color.WHITE; }
    public String passiveText() { return "Smash hits from 5+ blocks up, no fall damage"; }
    public String attackText() { return "Long lunge dash"; }

    @Override
    public void onDealDamage(Player p, EntityDamageByEntityEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        Material held = p.getInventory().getItemInMainHand().getType();
        double dmg = e.getDamage();
        if (held == Material.MACE) {
            e.setDamage(Math.min(dmg, MACE_CAP_HP)); // real mace: stronger cap
            return;
        }
        double fall = p.getFallDistance();
        if (fall <= 5) return;
        double bonus = smash(fall) * tier(held);
        e.setDamage(Math.min(dmg + bonus, CAP_HP));
        Location l = e.getEntity().getLocation();
        l.getWorld().spawnParticle(Particle.CLOUD, l, 25, 0.5, 0.2, 0.5, 0.1);
        l.getWorld().playSound(l, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
    }

    @Override
    public void onTakeDamage(Player p, EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) e.setCancelled(true);
    }

    // vanilla-style mace bonus (hp): 4/block for 3 blocks, 2/block for 5 more, 1/block after
    private double smash(double f) {
        return Math.min(f, 3) * 4 + Math.max(0, Math.min(f, 8) - 3) * 2 + Math.max(0, f - 8);
    }

    // heavily nerfed: better item -> more of the smash bonus
    private double tier(Material m) {
        String n = m.name();
        if (n.startsWith("NETHERITE")) return 0.6;
        if (n.startsWith("DIAMOND")) return 0.5;
        if (n.startsWith("IRON") || m == Material.TRIDENT) return 0.4;
        if (n.startsWith("STONE")) return 0.3;
        if (n.startsWith("GOLDEN") || n.startsWith("WOODEN")) return 0.2;
        return 0.1;
    }

    @Override
    public void attack(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.4f);
        Dash.run(plugin, p, 1.5, 9, pl -> {
            Location l = pl.getLocation().add(0, 1, 0);
            Fx.dust(l, color(), 1.5f, 6, 0.4);
            l.getWorld().spawnParticle(Particle.CLOUD, l, 4, 0.3, 0.3, 0.3, 0.02);
            Fx.orbit(l, color(), 1.0, 0);
        });
    }
}
