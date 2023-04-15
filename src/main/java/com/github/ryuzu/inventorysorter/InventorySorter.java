package com.github.ryuzu.inventorysorter;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class InventorySorter extends JavaPlugin implements Listener {
    private static HashMap<UUID, Long> cliked = new HashMap<>();
    private static InventorySorter instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (command.getName().equalsIgnoreCase("is")) toggle(p);
        }
        return false;
    }

    @EventHandler
    public void sort(InventoryClickEvent e) {
        if (e.getClickedInventory() != null) return;
        Inventory top = e.getView().getTopInventory();
        Inventory bottom = e.getView().getBottomInventory();
        Player p = (Player) e.getWhoClicked();
        if (!(top.getType().equals(InventoryType.CHEST) || top.getType().equals(InventoryType.ENDER_CHEST) || top.getType().equals(InventoryType.SHULKER_BOX) || top.getType().equals(InventoryType.BARREL)  || bottom.getType().equals(InventoryType.PLAYER)))
            return;
        Byte sort = p.getPersistentDataContainer().get(new NamespacedKey(this, "sort"), PersistentDataType.BYTE);
        if(sort != null && sort == 0)
            return;
        if (!(cliked.containsKey(p.getUniqueId()) && cliked.get(p.getUniqueId()) + 200 > System.currentTimeMillis()))
            cliked.put(p.getUniqueId(), System.currentTimeMillis());
        else {
            if (top.getType().equals(InventoryType.BARREL) || top.getType().equals(InventoryType.SHULKER_BOX) || top.getType().equals(InventoryType.ENDER_CHEST)  || (top.getType().equals(InventoryType.CHEST) && !top.getClass().getName().contains("Custom")))
                sort(top, 0, top.getSize());
            else
                sort(bottom, 9, 36);
            p.playSound(p.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1, 2);
            p.updateInventory();
        }
    }

    // sort inventory from start to end by material or display name alphabetically
    private static void sort(Inventory inv, int start, int end) {
        List<ItemStack> sortedItems = optimizeInventory(Arrays.stream(inv.getStorageContents()).skip(start).limit(end - start).filter(Objects::nonNull).collect(Collectors.toList()));
        for (int i = start; i < end; i++) inv.setItem(i, null);
        sortedItems.sort(sortInventory());
        for (int i = start; i < start + sortedItems.size(); i++) inv.setItem(i, sortedItems.get(i - start));
    }

    private static Comparator<ItemStack> sortByNanme() {
        return (item1, item2) -> {
            if (item1.isSimilar(item2))
                return item2.getAmount() - item1.getAmount();
            else if (hasDisplayName(item1) && hasDisplayName(item2))
                return comapreByNameAndMaxStackSize(ChatColor.stripColor(item1.getItemMeta().getDisplayName()), ChatColor.stripColor(item2.getItemMeta().getDisplayName()), item1.getType().getMaxStackSize(), item2.getType().getMaxStackSize());
            else if (hasDisplayName(item1))
                return -1;
            else if (hasDisplayName(item2))
                return 1;
            else if (item1.getType().equals(item2.getType()))
                return 0;
            else
                return comapreByNameAndMaxStackSize(item1.getType().name(), item2.getType().name(), item1.getType().getMaxStackSize(), item2.getType().getMaxStackSize());
        };
    }

    private static int comapreByNameAndMaxStackSize(String item1, String item2, int max1, int max2) {
        if(max1 == 1 && max2 != 1)
            return -1;
        else if(max1 != 1 && max2 == 1)
            return 1;
        else
            return item1.compareTo(item2);
    }

    private static Comparator<ItemStack> sortInventory() {
        return Comparator.comparing((ItemStack item) -> {
            // DisplayNameを持っているかどうかを判定
            if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                return 0;
            } else {
                return 1;
            }
        }).thenComparing((ItemStack item) -> {
            // MaxStackSizeが1かどうかを判定
            if (item.getMaxStackSize() == 1) {
                return 0;
            } else {
                return 1;
            }
        }).thenComparing((ItemStack item) -> {
            // Materialの名前順にソート
            return item.getType().name();
        }).thenComparing((ItemStack item) -> {
            // CustomModelDataの順にソート
            if (item.getItemMeta() != null && item.getItemMeta().hasCustomModelData()) {
                return item.getItemMeta().getCustomModelData();
            } else {
                return 0;
            }
        }).thenComparing((ItemStack item) -> {
            // DisplayNameの名前順にソート
            if (item.getItemMeta() != null && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            } else {
                return "";
            }
        });
    }

    private static boolean hasDisplayName(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName();
    }

    private static int getCustomModelData(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 0;
    }

    private static void toggle(Player p) {
        Byte bool = p.getPersistentDataContainer().get(new NamespacedKey(instance, "sort"), PersistentDataType.BYTE);
        if (bool != null && bool == 0) {
            p.getPersistentDataContainer().set(new NamespacedKey(instance, "sort"), PersistentDataType.BYTE, (byte) 1);
            p.sendMessage("§aソートを有効にしました");
        } else {
            p.getPersistentDataContainer().set(new NamespacedKey(instance, "sort"), PersistentDataType.BYTE, (byte) 0);
            p.sendMessage("§cソートを無効にしました");
        }
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
