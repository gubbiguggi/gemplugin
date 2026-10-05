package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Cooldowns shown as a boss bar that fills up while you wait. */
public class CooldownBar {
    private static final class Entry {
        final long start, end;
        final String label;
        final BossBar bar;
        Entry(long start, long end, String label, BossBar bar) {
            this.start = start; this.end = end; this.label = label; this.bar = bar;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public boolean ready(Player p) {
        Entry e = entries.get(p.getUniqueId());
        return e == null || System.currentTimeMillis() >= e.end;
    }

    public void start(Player p, long millis, String label, BossBar.Color color) {
        clear(p);
        long now = System.currentTimeMillis();
        BossBar bar = BossBar.bossBar(Component.text(label), 0f, color, BossBar.Overlay.PROGRESS);
        p.showBossBar(bar);
        entries.put(p.getUniqueId(), new Entry(now, now + millis, label, bar));
    }

    public void update() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Entry>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Entry> me = it.next();
            Entry e = me.getValue();
            Player p = Bukkit.getPlayer(me.getKey());
            if (now >= e.end) {
                if (p != null) p.hideBossBar(e.bar);
                it.remove();
                continue;
            }
            float prog = (float) (now - e.start) / (e.end - e.start);
            e.bar.progress(Math.max(0f, Math.min(1f, prog)));
            long secs = (e.end - now + 999) / 1000;
            e.bar.name(Component.text(e.label + " - " + secs + "s"));
        }
    }

    public void clear(Player p) {
        Entry e = entries.remove(p.getUniqueId());
        if (e != null) p.hideBossBar(e.bar);
    }

    /** Re-show the bar after a player rejoins (the cooldown keeps running while offline). */
    public void resume(Player p) {
        Entry e = entries.get(p.getUniqueId());
        if (e != null) p.showBossBar(e.bar);
    }
}
