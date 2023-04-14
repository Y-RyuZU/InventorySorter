package com.github.ryuzu.inventorysorter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        Player p = (Player) e.getWhoClicked();
        if (!(top.getType().equals(InventoryType.CHEST) || bottom.getType().equals(InventoryType.PLAYER)))
            return;
        if (!(cliked.containsKey(p.getUniqueId()) && cliked.get(p.getUniqueId()) + 200 > System.currentTimeMillis()))
            cliked.put(p.getUniqueId(), System.currentTimeMillis());
        else {
            if (top.getType().equals(InventoryType.CHEST) && !top.getClass().getName().contains("Custom"))
                sort(top, 0, top.getSize());
            else
                sort(bottom, 9, 36);
            p.playSound(p.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 2);
            p.updateInventory();
        }
    }

    // sort inventory from start to end by material or display name alphabetically
    private void sort(Inventory inv, int start, int end) {
        List<ItemStack> sortedItems = optimizeInventory(Arrays.stream(inv.getStorageContents()).skip(start).limit(end - start).filter(Objects::nonNull).collect(Collectors.toList()));
        for (int i = start; i < end; i++) inv.setItem(i, null);
        sortedItems.sort((item1, item2) -> {
            if (item1.isSimilar(item2))
                return item2.getAmount() - item1.getAmount();
            else if (hasDisplayName(item1) && hasDisplayName(item2))
                return comapreByMaxStackSize(ChatColor.stripColor(item1.getItemMeta().getDisplayName()), ChatColor.stripColor(item2.getItemMeta().getDisplayName()), item1.getType().getMaxStackSize(), item2.getType().getMaxStackSize());
            else if (hasDisplayName(item1))
                return -1;
            else if (hasDisplayName(item2))
                return 1;
            else if (item1.getType().equals(item2.getType()))
                return 0;
            else
                return comapreByMaxStackSize(item1.getType().name(), item2.getType().name(), item1.getType().getMaxStackSize(), item2.getType().getMaxStackSize());
        });
        for (int i = start; i < start + sortedItems.size(); i++) inv.setItem(i, sortedItems.get(i - start));
    }

    private int comapreByMaxStackSize(String item1, String item2, int max1, int max2) {
        if(max1 == 1 && max2 != 1)
            return -1;
        else if(max1 != 1 && max2 == 1)
            return 1;
        else
            return item1.compareTo(item2);
    }

    private boolean hasDisplayName(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }

    private static List<ItemStack> optimizeInventory(List<ItemStack> inventory) {
        Map<ItemStack, Integer> itemMap = inventory.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getType() != Material.AIR)
                .collect(Collectors.toMap(
                        item -> {
                            ItemStack clone = item.clone();
                            clone.setAmount(1);
                            return clone;
                        },
                        ItemStack::getAmount,
                        Integer::sum
                ));

        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemStack, Integer> entry : itemMap.entrySet()) {
            ItemStack item = entry.getKey();
            int count = entry.getValue();
            int maxStackSize = item.getType().getMaxStackSize();
            for (int i = 0; i < count; i += maxStackSize) {
                ItemStack newItem = item.clone();
                newItem.setAmount(Math.min(maxStackSize, count - i));
                result.add(newItem);
            }
        }

        return result;
    }
}
