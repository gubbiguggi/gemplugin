package com.example.gem;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ElementManager {
    public static final long COOLDOWN_MS = 30_000;

    private final GemPlugin plugin;
    private final Map<String, Element> registry = new LinkedHashMap<>();
    private final Map<UUID, String> chosen = new HashMap<>();
    private final CooldownBar abilityBar = new CooldownBar();
    private int n = 0;

    public ElementManager(GemPlugin plugin) { this.plugin = plugin; }

    public void register(Element e) { registry.put(e.id(), e); }
    public Element get(String id) { return registry.get(id); }
    public Collection<Element> all() { return registry.values(); }
    public CooldownBar abilityBar() { return abilityBar; }

    public Element of(Player p) {
        String id = chosen.get(p.getUniqueId());
        return id == null ? null : registry.get(id);
    }

    public void set(Player p, Element e) {
        Element old = of(p);
        if (old != null) old.onRemove(p);
        if (e == null) chosen.remove(p.getUniqueId());
        else chosen.put(p.getUniqueId(), e.id());
        save();
    }

    public boolean hasEgg(Player p) {
        return p.getInventory().contains(Material.DRAGON_EGG);
    }

    /** Right-click with the focus item. */
    public void use(Player p) {
        Element e = of(p);
        if (e == null) return;
        if (plugin.nullifier().isNullified(p)) {
            p.sendActionBar(Component.text("Your element is nullified!", NamedTextColor.RED));
            return;
        }
        if (Combat.isStunned(p)) return;
        if (!abilityBar.ready(p)) return; // the boss bar already shows the cooldown
        long ms = hasEgg(p) ? COOLDOWN_MS / 2 : COOLDOWN_MS; // dragon egg halves cooldowns
        abilityBar.start(p, ms, e.displayName(), e.barColor());
        e.attack(p);
    }

    public void startLoop() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            n++;
            plugin.nullifier().prune();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isDead()) continue;
                Element e = of(p);
                boolean active = e != null && !plugin.nullifier().isNullified(p);
                Map<PotionEffectType, Integer> fx = new HashMap<>();
                if (e != null) {
                    if (active) e.effects().forEach((t, a) -> fx.merge(t, a, Math::max));
                    e.tick(p, active, n);
                }
                if (hasEgg(p)) { // dragon egg bonuses
                    fx.merge(PotionEffectType.STRENGTH, 0, Math::max);
                    fx.merge(PotionEffectType.SPEED, 0, Math::max);
                    fx.merge(PotionEffectType.FIRE_RESISTANCE, 0, Math::max);
                }
                fx.forEach((t, a) -> p.addPotionEffect(new PotionEffect(t, 40, a, true, false, false)));
            }
            abilityBar.update();
            plugin.nullifierBar().update();
        }, 5L, 5L);
    }

    // ---- persistence ----
    private File file() { return new File(plugin.getDataFolder(), "players.yml"); }

    public void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file());
        for (String k : y.getKeys(false)) {
            try { chosen.put(UUID.fromString(k), y.getString(k)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        chosen.forEach((u, id) -> y.set(u.toString(), id));
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file());
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save players.yml: " + ex.getMessage());
        }
    }
}
