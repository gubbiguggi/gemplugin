package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EarthElement extends Element {
    private static final double RADIUS = 6;
    private final NamespacedKey mineKey, kbKey;

    public EarthElement(GemPlugin plugin) {
        super(plugin);
        mineKey = new NamespacedKey(plugin, "earth_mining");
        kbKey = new NamespacedKey(plugin, "earth_knockback");
    }

    public String id() { return "earth"; }
    public String displayName() { return "Earth"; }
    public Color color() { return Color.fromRGB(130, 95, 50); }
    public BossBar.Color barColor() { return BossBar.Color.GREEN; }
    public String passiveText() { return "Faster mining, no knockback"; }
    public String attackText() { return "Pillars erupt around you (3 hearts + slowness II)"; }

    // ---- passive: attribute modifiers (not haste, so attack speed is untouched) ----
    @Override
    public void tick(Player p, boolean active, int n) {
        if (n % 4 != 0) return;
        set(p, Attribute.BLOCK_BREAK_SPEED, mineKey, 1.0, AttributeModifier.Operation.ADD_SCALAR, active);
        set(p, Attribute.KNOCKBACK_RESISTANCE, kbKey, 1.0, AttributeModifier.Operation.ADD_NUMBER, active);
    }

    @Override
    public void onRemove(Player p) {
        set(p, Attribute.BLOCK_BREAK_SPEED, mineKey, 0, AttributeModifier.Operation.ADD_SCALAR, false);
        set(p, Attribute.KNOCKBACK_RESISTANCE, kbKey, 0, AttributeModifier.Operation.ADD_NUMBER, false);
    }

    private void set(Player p, Attribute a, NamespacedKey key, double amt, AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        AttributeModifier existing = null;
        for (AttributeModifier m : inst.getModifiers()) if (m.getKey().equals(key)) existing = m;
        if (on && existing == null) inst.addTransientModifier(new AttributeModifier(key, amt, op));
        else if (!on && existing != null) inst.removeModifier(existing);
    }

    // ---- attack ----
    @Override
    public void attack(Player p) {
        Location center = p.getLocation().clone();
        World w = center.getWorld();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<BlockDisplay> ds = new ArrayList<>();
        List<Location> bases = new ArrayList<>();
        List<Double> heights = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double r = Math.sqrt(rnd.nextDouble()) * RADIUS;
            int x = (int) Math.floor(center.getX() + Math.cos(a) * r);
            int z = (int) Math.floor(center.getZ() + Math.sin(a) * r);
            Block ground = findGround(w, x, center.getBlockY(), z);
            if (ground == null) continue;
            BlockData data = ground.getBlockData();
            Location base = new Location(w, x, ground.getY() - 2, z);
            BlockDisplay d = w.spawn(base, BlockDisplay.class, bd -> {
                bd.setBlock(data);
                bd.setPersistent(false);
                bd.setTeleportDuration(1);
                bd.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(1f, 3f, 1f), new AxisAngle4f()));
            });
            ds.add(d);
            bases.add(base);
            heights.add(0.9 + rnd.nextDouble() * 0.9);
            w.spawnParticle(Particle.BLOCK, new Location(w, x + 0.5, ground.getY() + 1, z + 0.5), 6, 0.3, 0.1, 0.3, data);
        }
        w.playSound(center, Sound.BLOCK_GRAVEL_BREAK, 1.5f, 0.5f);

        new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                double rise = t < 8 ? t * 0.2 : t < 16 ? 1.6 : Math.max(0, 1.6 - (t - 16) * 0.2);
                for (int i = 0; i < ds.size(); i++) {
                    ds.get(i).teleport(bases.get(i).clone().add(0, rise * heights.get(i) / 1.6, 0));
                }
                if (t == 4) smash(p, center);
                if (t % 3 == 0) Fx.ring(center.clone().add(0, 0.2, 0), RADIUS, 48, color(), 1.3f);
                if (++t > 24) {
                    ds.forEach(Entity::remove);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void smash(Player p, Location center) {
        for (LivingEntity t : Combat.targets(p, center, RADIUS)) {
            Combat.damage(t, p, 6, true);
            t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
            t.setVelocity(t.getVelocity().setY(0.7));
        }
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
    }

    private Block findGround(World w, int x, int y, int z) {
        for (int yy = y + 2; yy >= y - 4; yy--) {
            Block b = w.getBlockAt(x, yy, z);
            if (b.getType().isSolid() && b.getRelative(0, 1, 0).isPassable()) return b;
        }
        return null;
    }
}
