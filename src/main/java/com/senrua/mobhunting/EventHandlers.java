package com.senrua.mobhunting;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;

public class EventHandlers implements Listener {
    private final MobHunting mobHunting;

    public EventHandlers(MobHunting mobHunting) {
        this.mobHunting = mobHunting;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && item.getType() == Material.PLAYER_HEAD && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().endsWith(" Trophy")) {
                EntityType entityType = EntityType.fromName(ChatColor.stripColor(meta.getDisplayName().replace(" Trophy", "")));
                if (entityType != null && mobHunting.hasPlayerTrophy(player.getUniqueId(), entityType)) {
                    event.setCancelled(true);
                    if (event.getRightClicked() instanceof LivingEntity) {
                        LivingEntity entity = (LivingEntity) event.getRightClicked();
                        ItemStack trophyItem = mobHunting.getHeadItem(entityType);
                        if (trophyItem != null) {
                            trophyItem.setAmount(1);
                            entity.getWorld().dropItem(entity.getLocation(), trophyItem);
                            mobHunting.setPlayerTrophy(player.getUniqueId(), entityType, false);
                        }
                    }
                }
            }
        }
    }


    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String[] args = event.getMessage().split(" ");
        if (args.length > 0 && args[0].equalsIgnoreCase("/hunt")) {
            mobHunting.openHuntGUI(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        } else if (args.length > 0 && args[0].equalsIgnoreCase("/trophies")) {
            mobHunting.openTrophiesGUI(event.getPlayer().getUniqueId());
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        EntityType entityType = entity.getType();
        ItemStack headItem = mobHunting.getHeadItem(entityType);
        if (headItem != null) {
            double dropRate = mobHunting.getConfig().getDouble("mobs." + entityType.name() + ".drop-rate", 1.0);
            mobHunting.getLogger().info("Drop rate for head item: " + dropRate);
            if (Math.random() <= dropRate) {
                mobHunting.getLogger().info("Head item dropped for " + entityType.toString());
                entity.getWorld().dropItem(entity.getLocation(), headItem);
                if (entity.getKiller() != null && entity.getKiller().isOnline()) {
                    mobHunting.setPlayerTrophy(entity.getKiller().getUniqueId(), entityType, true);
                }
            } else {
                mobHunting.getLogger().info("Head item not dropped for " + entityType.toString());
            }
        }
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Trophies")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick2(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Hunt")) {
            event.setCancelled(true);
        }
    }
}
