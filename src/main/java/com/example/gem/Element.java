package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

public abstract class Element {
    protected final GemPlugin plugin;

    protected Element(GemPlugin plugin) { this.plugin = plugin; }

    public abstract String id();
    public abstract String displayName();
    public abstract Color color();
    public abstract BossBar.Color barColor();
    public abstract String passiveText();
    public abstract String attackText();
    public boolean legendary() { return false; }

    /** Permanent potion effects (re-applied every 5 ticks, merged with the dragon egg). */
    public Map<PotionEffectType, Integer> effects() { return Map.of(); }

    /** Called every 5 ticks for the player's element. active=false while nullified. n = tick counter. */
    public void tick(Player p, boolean active, int n) {}

    /** Called when the element is removed/changed so attribute modifiers etc. can be cleaned up. */
    public void onRemove(Player p) {}

    /** Called when this element's owner melee-hits something (only while active, never for ability damage). */
    public void onDealDamage(Player attacker, EntityDamageByEntityEvent e) {}

    /** Called when this element's owner takes damage (only while active). */
    public void onTakeDamage(Player victim, EntityDamageEvent e) {}

    public abstract void attack(Player p);
}
