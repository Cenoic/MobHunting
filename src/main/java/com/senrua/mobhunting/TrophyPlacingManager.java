package com.senrua.mobhunting;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TrophyPlacingManager {
    private MobHunting mobHunting;
    private TrophyPlacingManager instance;
    private FileConfiguration config;
    private File configFile;

    public TrophyPlacingManager getInstance(MobHunting mobHunting) {
        if (instance == null) {
            instance = new TrophyPlacingManager(mobHunting);
        }
        return instance;
    }

    public TrophyPlacingManager(MobHunting mobHunting) {
        this.mobHunting = mobHunting;
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(mobHunting.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defConfigStream = mobHunting.getResource("config.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            config.setDefaults(defConfig);
        }
    }

    public BlockFace getBlockFace(Player player) {
        Location location = player.getLocation();
        BlockFace face = location.getBlock().getFace(location.getBlock().getRelative(BlockFace.UP));
        return face;
    }

    public boolean placeTrophy(Player player, Location location, BlockFace face, ItemStack item) {
        Material itemType = item.getType();
        EntityType entityType = null;

        Bukkit.getLogger().info("Item type: " + itemType);

        if (itemType == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            if (skullMeta.hasOwner()) {
                String ownerName = skullMeta.getOwningPlayer().getName();
                Bukkit.getLogger().info("Head owner: " + ownerName);
                for (String mobName : getConfig().getConfigurationSection("mobs").getKeys(false)) {
                    String configMaterial = config.getString("Trophies." + mobName + ".material");
                    if (configMaterial != null && configMaterial.equalsIgnoreCase(itemType.toString())) {
                        entityType = EntityType.valueOf(mobName.toUpperCase());
                        break;
                    }
                }
            }
        } else {
            for (EntityType et : EntityType.values()) {
                if (et.name().equals(itemType.name())) {
                    entityType = et;
                    break;
                }
            }
        }

        Bukkit.getLogger().info("Entity type: " + entityType);

        if (entityType != null) {
            String trophyItemName = getConfig().getString("mobs." + entityType.name() + ".trophy-item");
            Material trophyMaterial = Material.getMaterial(trophyItemName);
            Bukkit.getLogger().info("Trophy material: " + trophyMaterial);

            if (trophyMaterial == null) {
                player.sendMessage("Invalid trophy item for mob: " + entityType.name());
                return false;
            }

            String headItemName = getConfig().getString("mobs." + entityType.name() + ".head-item");
            Material headMaterial = Material.getMaterial(headItemName);
            Bukkit.getLogger().info("Head material: " + headMaterial);

            if (headMaterial == null) {
                player.sendMessage("Invalid head item for mob: " + entityType.name());
                return false;
            }

            ItemStack requiredHeadItem = new ItemStack(headMaterial);
            ItemMeta meta = requiredHeadItem.getItemMeta();
            meta.setDisplayName(entityType.name() + " Head");
            requiredHeadItem.setItemMeta(meta);
            int count = player.getInventory().all(requiredHeadItem).values().stream().mapToInt(ItemStack::getAmount).sum();

            Bukkit.getLogger().info("Required head item count: " + count);

            if (count < 1) {
                player.sendMessage("You don't have the required head item for mob: " + entityType.name());
                return false;
            }

            ItemStack trophyItem = new ItemStack(trophyMaterial);
            SkullMeta skullMeta = (SkullMeta) trophyItem.getItemMeta();
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(getConfig().getString("mobs." + entityType.name() + ".head-owner")));
            trophyItem.setItemMeta(skullMeta);

            location.getBlock().setType(trophyMaterial);
            BlockState blockState = location.getBlock().getState();

            Bukkit.getLogger().info("Block state: " + blockState);
            if (blockState instanceof Skull) {
                Skull skull = (Skull) blockState;
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(getConfig().getString("mobs." + entityType.name() + ".head-owner")));
                skull.setRotation(face);
                skull.update();
                player.getInventory().removeItem(requiredHeadItem);
                location.getWorld().playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                player.sendMessage("Trophy placed successfully.");
                return true;
            } else {
                player.sendMessage("Failed to place trophy.");
                return false;
            }
        } else {
            player.sendMessage("Invalid trophy item. Looking for: " + getConfig().getConfigurationSection("mobs").getKeys(false) + ", but received: " + itemType.name());
            return false;
        }
    }
}
