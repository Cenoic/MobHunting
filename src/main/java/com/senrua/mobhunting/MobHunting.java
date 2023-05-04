package com.senrua.mobhunting;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.Inventory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
public class MobHunting extends JavaPlugin implements Listener, CommandExecutor {
    private static MobHunting instance;
    private Map<UUID, Map<EntityType, Boolean>> playerTrophies;
    private YamlConfiguration playerTrophiesConfig;
    private TrophyPlacingManager trophyPlacingManager;
    @Override
    public void onEnable() {
        trophyPlacingManager = new TrophyPlacingManager(this);

        getServer().getPluginManager().registerEvents(new EventHandlers(this), this);
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        loadPlayerTrophies();
        registerEvents();
        registerCommands();
        startSaveTask();
    }


    @Override
    public void onDisable() {
        savePlayerTrophies();
    }
    public MobHunting() {
        this.trophyPlacingManager = new TrophyPlacingManager(this);
    }
    public TrophyPlacingManager getTrophyPlacingManager() {
        return trophyPlacingManager;
    }


    public void loadPlayerTrophies() {
        playerTrophies = new HashMap<>();
        File configFile = new File(getDataFolder(), "player_trophies.yml");
        playerTrophiesConfig = YamlConfiguration.loadConfiguration(configFile);
        for (String uuidStr : playerTrophiesConfig.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            Map<EntityType, Boolean> trophies = new HashMap<>();
            for (String entityTypeStr : playerTrophiesConfig.getConfigurationSection(uuidStr).getKeys(false)) {
                trophies.put(EntityType.fromName(entityTypeStr), playerTrophiesConfig.getBoolean(uuidStr + "." + entityTypeStr));
            }
            playerTrophies.put(uuid, trophies);
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void registerCommands() {
        getCommand("hunt").setExecutor(this);
        getCommand("trophies").setExecutor(this);
        getCommand("displaytrophy").setExecutor(this);
    }

    private void startSaveTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                savePlayerTrophies();
            }
        }.runTaskTimer(this, 0, 6000);
    }

    public ItemStack getHeadItem(EntityType entityType) {
        ConfigurationSection mobConfig = getConfig().getConfigurationSection("mobs." + entityType.name());
        if (mobConfig != null) {
            String headItemName = mobConfig.getString("head-item");
            if (headItemName != null && !headItemName.isEmpty()) {
                Material headItemType = Material.matchMaterial(headItemName);
                if (headItemType == null) {
                    headItemType = Material.PLAYER_HEAD;
                }
                ItemStack headItem = new ItemStack(headItemType);
                ItemMeta headItemMeta = headItem.getItemMeta();
                headItemMeta.setDisplayName(entityType.name().toUpperCase() + " Trophy");
                if (headItemType == Material.PLAYER_HEAD) {
                    SkullMeta skullMeta = (SkullMeta) headItemMeta;
                    String headOwner = mobConfig.getString("head-owner");
                    if (headOwner != null && !headOwner.isEmpty()) {
                        skullMeta.setOwningPlayer(getServer().getOfflinePlayer(headOwner));
                    } else {
                        skullMeta.setOwningPlayer(getServer().getOfflinePlayer("Notch")); // Change to actual player's name
                    }
                }
                headItem.setItemMeta(headItemMeta);
                double dropRate = mobConfig.getDouble("drop-rate", 1.0);
                if (dropRate >= 1.0 || Math.random() <= dropRate) { // Check if drop rate is 1.0 or if random value is less than or equal to drop rate
                    return headItem;
                }
            }
        }
        return null;
    }


    public void setPlayerTrophy(UUID uuid, EntityType entityType, boolean value) {
        Map<EntityType, Boolean> trophies = playerTrophies.computeIfAbsent(uuid, k -> new HashMap<>());
        trophies.put(entityType, value);
        if (value) { // Check if the trophy was collected (value is true)
            Player player = getPlayer(uuid);
            if (player != null) {
                PermissionAttachment attachment = player.addAttachment(this);
                attachment.setPermission("mobhunting." + entityType.name().toLowerCase(), true);
            }
        }
    }


    public boolean hasPlayerTrophy(UUID uuid, EntityType entityType) {
        Map<EntityType, Boolean> trophies = playerTrophies.get(uuid);
        return trophies != null && trophies.get(entityType) != null && trophies.get(entityType);
    }

    public void openHuntGUI(UUID uuid) {
        Inventory gui = getServer().createInventory(null, 54, "Hunt");
        ConfigurationSection mobsConfig = getConfig().getConfigurationSection("mobs");
        for (EntityType entityType : EntityType.values()) {
            if (getHeadItem(entityType) != null) {
                if (!hasPermission(uuid, entityType)) {
                    if (mobsConfig.contains(entityType.name() + ".permission") &&
                            !getPlayer(uuid).hasPermission(mobsConfig.getString(entityType.name() + ".permission"))) {
                        ItemStack headItem = getHeadItem(entityType, mobsConfig);
                        gui.addItem(headItem);
                    }
                }
            }
        }
        getPlayer(uuid).openInventory(gui);
    }

    public ItemStack getHeadItem(EntityType entityType, ConfigurationSection mobsConfig) {
        ConfigurationSection mobConfig = mobsConfig.getConfigurationSection(entityType.name());
        if (mobConfig != null) {
            String headItemName = mobConfig.getString("head-item");
            if (headItemName != null && !headItemName.isEmpty()) {
                Material headItemType = Material.matchMaterial(headItemName);
                if (headItemType == null) {
                    headItemType = Material.PLAYER_HEAD;
                }
                ItemStack headItem = new ItemStack(headItemType);
                ItemMeta headItemMeta = headItem.getItemMeta();
                headItemMeta.setDisplayName(entityType.name().toUpperCase() + " Trophy");
                if (headItemType == Material.PLAYER_HEAD) {
                    SkullMeta skullMeta = (SkullMeta) headItemMeta;
                    String headOwner = mobConfig.getString("head-owner");
                    if (headOwner != null && !headOwner.isEmpty()) {
                        skullMeta.setOwningPlayer(getServer().getOfflinePlayer(headOwner));
                    } else {
                        skullMeta.setOwningPlayer(getServer().getOfflinePlayer("Notch")); // Change to actual player's name
                    }
                }
                headItem.setItemMeta(headItemMeta);
                double dropRate = mobConfig.getDouble("drop-rate", 1.0);
                if (dropRate < 1.0) {
                    if (Math.random() > dropRate) {
                        return null;
                    }
                }
                return headItem;
            }
        }
        return null;
    }

    public void openTrophiesGUI(UUID uuid) {
        Inventory gui = getServer().createInventory(null, 54, "Trophies");
        for (EntityType entityType : EntityType.values()) {
            if (getHeadItem(entityType) != null && hasPlayerTrophy(uuid, entityType)) {
                ItemStack headItem = getHeadItem(entityType);
                ItemMeta headItemMeta = headItem.getItemMeta();
                headItemMeta.setLore(Arrays.asList(ChatColor.GREEN + "Collected"));
                headItem.setItemMeta(headItemMeta);
                gui.addItem(headItem);
            }
        }
        getPlayer(uuid).openInventory(gui);
    }
    public boolean hasPermission(UUID uuid, EntityType entityType) {
        return getPlayer(uuid).hasPermission("mobhunting.hunt." + entityType.name().toLowerCase());
    }

    public void savePlayerTrophies() {
        for (UUID uuid : playerTrophies.keySet()) {
            Map<EntityType, Boolean> trophies = playerTrophies.get(uuid);
            for (EntityType entityType : trophies.keySet()) {
                playerTrophiesConfig.set(uuid.toString() + "." + entityType.name(), trophies.get(entityType));
            }
        }
        try {
            playerTrophiesConfig.save(new File(getDataFolder(), "player_trophies.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Player getPlayer(UUID uuid) {
        return getServer().getPlayer(uuid);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hunt")) {
            openHuntGUI(((Player) sender).getUniqueId());
            return true;
        } else if (command.getName().equalsIgnoreCase("trophies")) {
            openTrophiesGUI(((Player) sender).getUniqueId());
            return true;
        } else if (command.getName().equalsIgnoreCase("displaytrophy")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "Please specify a mob name.");
                return true;
            }

            String mobName = args[0].toUpperCase();
            String trophyDisplayName = mobName + " Trophy";

            // Check if the player has the trophy with the specified display name in their inventory
            ItemStack trophyItem = null;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && trophyDisplayName.equals(item.getItemMeta().getDisplayName())) {
                    trophyItem = item;
                    break;
                }
            }

            if (trophyItem == null) {
                player.sendMessage(ChatColor.RED + "You don't have the " + mobName + " trophy in your inventory.");
                return true;
            }

// Get the location where the player is looking
            Location location = player.getTargetBlockExact(10).getLocation();
            if (location == null) {
                player.sendMessage(ChatColor.RED + "You must be looking at a block to place a trophy.");
                return true;
            }

// Get the block where the player is looking
            Block targetBlock = location.getBlock();

// Get the block face where the player is looking
            BlockFace blockFace = player.getFacing();

// Calculate the block location where the trophy should be placed
            Location trophyLocation;
            if (blockFace == BlockFace.UP && !targetBlock.getType().isSolid()) {
                // Place on top of the target block
                trophyLocation = targetBlock.getRelative(BlockFace.UP).getLocation();
            } else {
                // Place on the side of the target block where the player is looking
                trophyLocation = targetBlock.getRelative(blockFace).getLocation();

                // Check if the trophy should be placed on the opposite side of the block
                if (trophyLocation.getBlock().getType().isSolid()) {
                    blockFace = blockFace.getOppositeFace();
                    trophyLocation = targetBlock.getRelative(blockFace).getLocation();
                }
            }

// Place the trophy block at the calculated location
            Block trophyBlock = trophyLocation.getBlock();
            trophyBlock.setType(Material.PLAYER_HEAD);
            BlockState blockState = trophyBlock.getState();

            if (blockState instanceof Skull) {
                Skull skull = (Skull) blockState;

                // Get the head-owner value from the config file
                String headOwner = getConfig().getString("mobs." + mobName + ".head-owner");
                if (headOwner == null) {
                    player.sendMessage(ChatColor.RED + "Invalid mob name. Please check the config file for valid mob names.");
                    return true;
                }

                // Set the skull's owner using the head-owner value from the config file
                OfflinePlayer mobHeadPlayer = Bukkit.getOfflinePlayer(headOwner);
                skull.setOwningPlayer(mobHeadPlayer);

                // Set the rotation of the skull to match the player's facing direction
                skull.setRotation(blockFace);

                // Update the skull block
                skull.update(true, false);

                // Remove the trophy from the player's inventory
                player.getInventory().removeItem(trophyItem);
                player.sendMessage(ChatColor.GREEN + "Trophy for " + mobName + " has been displayed!");

                // Play sound
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            return true;
        }
        return false;
    }

    public class ChestGUI {
        private Inventory gui;
        private UUID uuid;

        public ChestGUI(UUID uuid) {
            this.uuid = uuid;
            gui = getServer().createInventory(null, 54, "Hunt");
            populateGUI();
        }

        public void open() {
            getPlayer(uuid).openInventory(gui);
        }

        private void populateGUI() {
            for (EntityType entityType : EntityType.values()) {
                if (getHeadItem(entityType) != null && hasPermission(uuid, entityType)) {
                    gui.addItem(getHeadItem(entityType));
                }
            }
        }
    }
}