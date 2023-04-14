package com.github.ryuzu.inventorysorter;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.*;

public final class InventorySorter extends JavaPlugin implements Listener {
    private static HashMap<UUID, Long> cliked = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void sort(InventoryClickEvent e) {
        if (e.getClickedInventory() != null) return;
        Inventory inv = e.getView().getTopInventory();
        if (!(
                (inv.getType().equals(InventoryType.CHEST)) ||
                inv instanceof DoubleChestInventory ||
                inv instanceof PlayerInventory
        )) return;
        if(!(cliked.containsKey(e.getWhoClicked().getUniqueId()) && cliked.get(e.getWhoClicked().getUniqueId()) + 100 > System.currentTimeMillis()))
            cliked.put(e.getWhoClicked().getUniqueId(), System.currentTimeMillis());
        else  {
            if(inv instanceof PlayerInventory)
                sort(inv, 9, 36);
            else
                sort(inv, 0, inv.getSize());
        }
    }

    // sort inventory from start to end by material or display name alphabetically
    private void sort(Inventory inv, int start, int end) {
        List<ItemStack> sortedItems = new ArrayList<>();
        Arrays.stream(inv.getContents()).skip(start).limit(end - start).filter(Objects::nonNull).forEach(sortedItems::add);
        for(int i = start; i < end; i++) inv.setItem(i, null);
        sortedItems.sort((item1, item2) -> {
            if(hasDisplayName(item1) && hasDisplayName(item2))
                return ChatColor.stripColor(item1.getItemMeta().getDisplayName()).compareTo(ChatColor.stripColor(item2.getItemMeta().getDisplayName()));
            else if (hasDisplayName(item1))
                return -1;
            else if (hasDisplayName(item2))
                return 1;
            else if (item1.getType().equals(item2.getType()))
                return 0;
            else
                return item1.getType().name().compareTo(item2.getType().name());
        });
        inv.addItem(sortedItems.toArray(new ItemStack[0]));
    }

    private boolean hasDisplayName(@Nullable ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }
}
