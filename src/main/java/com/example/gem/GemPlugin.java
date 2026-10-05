package com.example.gem;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GemPlugin extends JavaPlugin implements TabExecutor {
    public static final String PACK_NAMESPACE = "gemplugin";

    public NamespacedKey focusKey;
    public NamespacedKey nullifierKey;
    private ElementManager manager;
    private NullifierManager nullifier;
    private final CooldownBar nullifierBar = new CooldownBar();

    public ElementManager manager() { return manager; }
    public NullifierManager nullifier() { return nullifier; }
    public CooldownBar nullifierBar() { return nullifierBar; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        focusKey = new NamespacedKey(this, "element_focus");
        nullifierKey = new NamespacedKey(this, "nullifier");
        nullifier = new NullifierManager(this);
        manager = new ElementManager(this);

        manager.register(new FireElement(this));
        manager.register(new WindElement(this));
        manager.register(new WaterElement(this));
        manager.register(new EarthElement(this));
        manager.register(new LightningElement(this));
        manager.register(new IceElement(this));
        manager.register(new LightElement(this));
        manager.register(new DarknessElement(this));
        manager.register(new TimeElement(this));
        manager.load();

        getServer().getPluginManager().registerEvents(new GemListener(this), this);
        getCommand("element").setExecutor(this);
        getCommand("element").setTabCompleter(this);
        getCommand("nullifier").setExecutor(this);
        manager.startLoop();
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Element e = manager.of(p);
            if (e != null) e.onRemove(p);
        }
        manager.save();
    }

    // ---------- items ----------
    public ItemStack modelItem(String model) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.setItemModel(new NamespacedKey(PACK_NAMESPACE, model));
        it.setItemMeta(m);
        return it;
    }

    private Component plain(String s) {
        return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public ItemStack focusItem(Element e) {
        ItemStack it = modelItem(e.id());
        ItemMeta m = it.getItemMeta();
        TextColor col = TextColor.color(e.color().asRGB());
        Component name = Component.text(e.displayName() + " Focus", col).decoration(TextDecoration.ITALIC, false);
        if (e.legendary()) name = name.decorate(TextDecoration.BOLD);
        m.displayName(name);
        m.lore(List.of(
                plain("Passive: " + e.passiveText()),
                plain("Ability: " + e.attackText()),
                plain(e.legendary() ? "Legendary - right-click to use" : "Right-click to use")));
        m.getPersistentDataContainer().set(focusKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    public ItemStack nullifierItem() {
        ItemStack it = modelItem("nullifier");
        ItemMeta m = it.getItemMeta();
        m.displayName(Component.text("Element Nullifier", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                plain("Disables other players' elements in a 12 block radius"),
                plain("Lasts 30s, 60s cooldown")));
        m.getPersistentDataContainer().set(nullifierKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    public void useNullifier(Player p) {
        if (!nullifierBar.ready(p)) return;
        nullifierBar.start(p, NullifierManager.COOLDOWN_MS, "Element Nullifier", BossBar.Color.WHITE);
        nullifier.create(p);
    }

    // ---------- commands ----------
    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (c.getName().equalsIgnoreCase("nullifier")) {
            Player t = a.length > 0 ? Bukkit.getPlayerExact(a[0]) : (s instanceof Player p ? p : null);
            if (t == null) { s.sendMessage("Usage: /nullifier [player]"); return true; }
            t.getInventory().addItem(nullifierItem());
            return true;
        }

        Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
        if (a.length == 0 || t == null) {
            s.sendMessage("Usage: /element <" + String.join("|", ids()) + "|none> [player]");
            return true;
        }
        if (a[0].equalsIgnoreCase("none")) {
            manager.set(t, null);
            s.sendMessage(t.getName() + " no longer has an element.");
            return true;
        }
        Element e = manager.get(a[0].toLowerCase());
        if (e == null) { s.sendMessage("Unknown element. Options: " + String.join(", ", ids())); return true; }
        manager.set(t, e);
        t.getInventory().addItem(focusItem(e));
        t.sendMessage(Component.text("You are now a " + e.displayName() + " user.",
                TextColor.color(e.color().asRGB())));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (a.length == 1 && c.getName().equalsIgnoreCase("element")) {
            List<String> out = ids();
            out.add("none");
            return out.stream().filter(x -> x.startsWith(a[0].toLowerCase())).toList();
        }
        return List.of();
    }

    private List<String> ids() {
        List<String> ids = new ArrayList<>();
        manager.all().forEach(e -> ids.add(e.id()));
        return ids;
    }
}
