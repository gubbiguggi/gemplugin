package com.example.gem;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class GemListener implements Listener {
    private final GemPlugin plugin;

    public GemListener(GemPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!e.getAction().isRightClick() || e.getHand() != EquipmentSlot.HAND) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        PersistentDataContainer pdc = it.getItemMeta().getPersistentDataContainer();
        if (pdc.has(plugin.focusKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            plugin.manager().use(e.getPlayer());
        } else if (pdc.has(plugin.nullifierKey, PersistentDataType.BYTE)) {
            e.setCancelled(true);
            plugin.useNullifier(e.getPlayer());
        }
    }

    private Element active(Player p) {
        Element el = plugin.manager().of(p);
        return (el != null && !plugin.nullifier().isNullified(p)) ? el : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent e) {
        if (Combat.isInternal() || !(e.getDamager() instanceof Player p)) return;
        Element el = active(p);
        if (el != null) el.onDealDamage(p, e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Element el = active(p);
        if (el != null) el.onTakeDamage(p, e);
    }

    /** Stunned players can look around but not walk or jump. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!Combat.isStunned(e.getPlayer())) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (from.getX() == to.getX() && from.getZ() == to.getZ() && to.getY() <= from.getY()) return;
        Location fix = from.clone();
        fix.setYaw(to.getYaw());
        fix.setPitch(to.getPitch());
        if (to.getY() < from.getY()) fix.setY(to.getY());
        e.setTo(fix);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.manager().abilityBar().resume(p);
        plugin.nullifierBar().resume(p);
        String url = plugin.getConfig().getString("resource-pack.url", "");
        String sha = plugin.getConfig().getString("resource-pack.sha1", "");
        if (!url.isBlank()) p.setResourcePack(url, sha.isBlank() ? null : sha);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Element el = plugin.manager().of(e.getPlayer());
        if (el != null) el.onRemove(e.getPlayer());
    }
}
