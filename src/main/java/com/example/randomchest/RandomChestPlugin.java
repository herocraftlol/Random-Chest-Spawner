package com.example.randomchest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class RandomChestPlugin extends JavaPlugin implements Listener {

    private final Random random = new Random();
    private BukkitTask spawnTask = null;
    private BukkitTask despawnTask = null;
    private final AtomicBoolean isSpawning = new AtomicBoolean(false);

    // Materials grouped by rarity tier instead of one big flat pool
    private final Map<LootTier, List<Material>> materialsByTier = new HashMap<>();

    // Anti-abuse caps: limits how many "good" items a single chest can roll
    private static final int MAX_EPIC_PER_CHEST = 1;
    private static final int MAX_RARE_PER_CHEST = 2;

    // Track spawned chests and if they've been looted
    private final Map<Location, Boolean> spawnedChests = new HashMap<>();

    /**
     * Loot rarity tiers.
     * weight    = relative chance of this tier being picked for a given slot (out of the sum of all weights)
     * minAmount/maxAmount = stack size range rolled for this tier (later clamped to the material's real max stack size)
     */
    private enum LootTier {
        COMMON(58, 16, 64),    // bulk basics: stone, wood, dirt, basic food... half a stack to a full stack
        UNCOMMON(30, 4, 16),   // iron/gold tier gear & resources, decent food, redstone components
        RARE(9, 1, 4),         // diamond tier gear, rare drops, hard-to-get utility items
        EPIC(3, 1, 1);         // netherite, elytra, totem, nether star... the jackpot, single item only

        final int weight;
        final int minAmount;
        final int maxAmount;

        LootTier(int weight, int minAmount, int maxAmount) {
            this.weight = weight;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
        }
    }

    @Override
    public void onEnable() {
        initializeValidMaterials();
        getServer().getPluginManager().registerEvents(this, this);
        // Don't start spawn task here - it starts when a player joins
        getLogger().info("RandomChestPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        getLogger().info("RandomChestPlugin has been disabled!");
    }

    private void initializeValidMaterials() {
        // Only survival-obtainable items, grouped by rarity tier.
        // COMMON: bulk basics players already swim in -> handed out in big stacks (half a stack to a full stack).
        addToTier(LootTier.COMMON,
            "COAL", "CHARCOAL",
            "COBBLESTONE", "STONE", "GRANITE", "DIORITE", "ANDESITE", "DEEPSLATE", "TUFF", "CALCITE", "DRIPSTONE_BLOCK", "POWDER_SNOW_BALL",
            "GRAVEL", "SAND", "DIRT", "GRASS_BLOCK", "ROOTED_DIRT", "MOSS_BLOCK", "CLAY_BALL", "FLINT", "NETHERRACK", "BASALT", "BLACKSTONE", "END_STONE",
            "OAK_LOG", "SPRUCE_LOG", "BIRCH_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "MANGROVE_LOG", "CHERRY_LOG",
            "OAK_PLANKS", "SPRUCE_PLANKS", "BIRCH_PLANKS", "JUNGLE_PLANKS", "ACACIA_PLANKS", "DARK_OAK_PLANKS", "MANGROVE_PLANKS", "CHERRY_PLANKS", "CRIMSON_PLANKS", "WARPED_PLANKS",
            "OAK_SLAB", "SPRUCE_SLAB", "BIRCH_SLAB", "JUNGLE_SLAB", "ACACIA_SLAB", "DARK_OAK_SLAB", "MANGROVE_SLAB", "CHERRY_SLAB",
            "OAK_STAIRS", "SPRUCE_STAIRS", "BIRCH_STAIRS", "JUNGLE_STAIRS", "ACACIA_STAIRS", "DARK_OAK_STAIRS", "MANGROVE_STAIRS", "CHERRY_STAIRS",
            "OAK_FENCE", "SPRUCE_FENCE", "BIRCH_FENCE", "JUNGLE_FENCE", "ACACIA_FENCE", "DARK_OAK_FENCE", "MANGROVE_FENCE", "NETHER_BRICK_FENCE",
            "GLASS", "GLASS_PANE", "GLASS_BOTTLE",
            "BRICK", "NETHER_BRICK",
            "SNOW_BLOCK", "ICE", "PACKED_ICE",
            "COBBLED_DEEPSLATE", "DEEPSLATE_BRICKS", "DEEPSLATE_TILES",
            "POLISHED_GRANITE", "POLISHED_DIORITE", "POLISHED_ANDESITE", "POLISHED_DEEPSLATE", "CHISELED_DEEPSLATE",
            "COBBLESTONE_SLAB", "COBBLESTONE_STAIRS", "STONE_SLAB", "STONE_STAIRS", "SMOOTH_STONE", "SANDSTONE", "RED_SANDSTONE",
            "TERRACOTTA", "WHITE_TERRACOTTA", "ORANGE_TERRACOTTA", "MAGENTA_TERRACOTTA", "LIGHT_BLUE_TERRACOTTA", "YELLOW_TERRACOTTA",
            "LIME_TERRACOTTA", "PINK_TERRACOTTA", "GRAY_TERRACOTTA", "LIGHT_GRAY_TERRACOTTA", "CYAN_TERRACOTTA", "PURPLE_TERRACOTTA",
            "BLUE_TERRACOTTA", "BROWN_TERRACOTTA", "GREEN_TERRACOTTA", "RED_TERRACOTTA", "BLACK_TERRACOTTA",
            "WHITE_CONCRETE", "ORANGE_CONCRETE", "MAGENTA_CONCRETE", "LIGHT_BLUE_CONCRETE", "YELLOW_CONCRETE", "LIME_CONCRETE",
            "PINK_CONCRETE", "GRAY_CONCRETE", "LIGHT_GRAY_CONCRETE", "CYAN_CONCRETE", "PURPLE_CONCRETE", "BLUE_CONCRETE",
            "BROWN_CONCRETE", "GREEN_CONCRETE", "RED_CONCRETE", "BLACK_CONCRETE",
            "WHITE_CONCRETE_POWDER", "ORANGE_CONCRETE_POWDER", "MAGENTA_CONCRETE_POWDER", "LIGHT_BLUE_CONCRETE_POWDER", "YELLOW_CONCRETE_POWDER",
            "LIME_CONCRETE_POWDER", "PINK_CONCRETE_POWDER", "GRAY_CONCRETE_POWDER", "LIGHT_GRAY_CONCRETE_POWDER", "CYAN_CONCRETE_POWDER",
            "PURPLE_CONCRETE_POWDER", "BLUE_CONCRETE_POWDER", "BROWN_CONCRETE_POWDER", "GREEN_CONCRETE_POWDER", "RED_CONCRETE_POWDER", "BLACK_CONCRETE_POWDER",
            "WHITE_WOOL", "ORANGE_WOOL", "MAGENTA_WOOL", "LIGHT_BLUE_WOOL", "YELLOW_WOOL", "LIME_WOOL", "PINK_WOOL", "GRAY_WOOL",
            "LIGHT_GRAY_WOOL", "CYAN_WOOL", "PURPLE_WOOL", "BLUE_WOOL", "BROWN_WOOL", "GREEN_WOOL", "RED_WOOL", "BLACK_WOOL",
            "WHITE_BED", "ORANGE_BED", "MAGENTA_BED", "LIGHT_BLUE_BED", "YELLOW_BED", "LIME_BED", "PINK_BED", "GRAY_BED",
            "LIGHT_GRAY_BED", "CYAN_BED", "PURPLE_BED", "BLUE_BED", "BROWN_BED", "GREEN_BED", "RED_BED", "BLACK_BED",
            "WOODEN_SWORD", "STONE_SWORD", "WOODEN_PICKAXE", "STONE_PICKAXE", "WOODEN_AXE", "STONE_AXE", "WOODEN_SHOVEL", "STONE_SHOVEL", "WOODEN_HOE", "STONE_HOE",
            "LEATHER_HELMET", "LEATHER_CHESTPLATE", "LEATHER_LEGGINGS", "LEATHER_BOOTS",
            "APPLE", "BREAD", "COOKIE", "CARROT", "POTATO", "BAKED_POTATO", "POISONOUS_POTATO", "BEETROOT",
            "COOKED_BEEF", "BEEF", "COOKED_PORKCHOP", "PORKCHOP", "COOKED_CHICKEN", "CHICKEN", "COOKED_MUTTON", "MUTTON",
            "COOKED_RABBIT", "RABBIT", "COOKED_COD", "COD", "COOKED_SALMON", "SALMON",
            "MELON_SLICE", "DRIED_KELP", "SWEET_BERRIES", "GLOW_BERRIES",
            "PUMPKIN_SEEDS", "MELON_SEEDS", "WHEAT_SEEDS", "BEETROOT_SEEDS",
            "STRING", "FEATHER", "BONE", "BONE_MEAL", "GUNPOWDER", "SNOWBALL", "EGG",
            "LADDER", "RAIL", "TORCH", "SOUL_TORCH", "CANDLE", "SOUL_CANDLE",
            "LEVER", "STONE_BUTTON", "OAK_BUTTON", "STONE_PRESSURE_PLATE", "OAK_PRESSURE_PLATE",
            "FLOWER_POT", "MOSS_CARPET",
            "MELON", "PUMPKIN", "CARVED_PUMPKIN", "JACK_O_LANTERN",
            "ROTTEN_FLESH",
            "SOUL_SAND", "MYCELIUM", "PODZOL", "DIRT_PATH", "GRASS_PATH",
            "OAK_SAPLING", "SPRUCE_SAPLING", "BIRCH_SAPLING", "JUNGLE_SAPLING", "ACACIA_SAPLING", "DARK_OAK_SAPLING",
            "MANGROVE_PROPAGULE", "CHERRY_SAPLING", "AZALEA", "FLOWERING_AZALEA",
            "VINE", "LILY_PAD", "SMALL_DRIPLEAF", "BIG_DRIPLEAF", "HANGING_ROOTS",
            "GRASS", "TALL_GRASS", "FERN", "LARGE_FERN", "DEAD_BUSH", "SEAGRASS", "KELP",
            "BROWN_MUSHROOM", "RED_MUSHROOM", "NETHER_SPROUTS", "CRIMSON_ROOTS", "WARPED_ROOTS",
            "WHITE_DYE", "ORANGE_DYE", "MAGENTA_DYE", "LIGHT_BLUE_DYE", "YELLOW_DYE", "LIME_DYE", "PINK_DYE", "GRAY_DYE",
            "LIGHT_GRAY_DYE", "CYAN_DYE", "PURPLE_DYE", "BLUE_DYE", "BROWN_DYE", "GREEN_DYE", "RED_DYE", "BLACK_DYE",
            "COCOA_BEANS", "INK_SAC",
            "OAK_SIGN", "SPRUCE_SIGN", "BIRCH_SIGN", "JUNGLE_SIGN", "ACACIA_SIGN", "DARK_OAK_SIGN", "MANGROVE_SIGN", "CHERRY_SIGN",
            "LEAD"
        );

        // UNCOMMON: mid-tier resources & gear (iron/gold level), decent food, redstone & utility blocks.
        addToTier(LootTier.UNCOMMON,
            "IRON_INGOT", "GOLD_INGOT", "COPPER_INGOT", "RAW_IRON", "RAW_GOLD", "RAW_COPPER",
            "REDSTONE", "LAPIS_LAZULI", "AMETHYST_SHARD", "NETHER_QUARTZ", "GLOWSTONE", "OBSIDIAN",
            "PRISMARINE", "PRISMARINE_SHARD", "PRISMARINE_CRYSTALS", "SEA_LANTERN",
            "COAL_BLOCK", "COPPER_BLOCK", "REDSTONE_BLOCK", "LAPIS_BLOCK", "QUARTZ_BLOCK", "IRON_BLOCK", "GOLD_BLOCK",
            "IRON_SWORD", "IRON_PICKAXE", "IRON_AXE", "IRON_SHOVEL", "IRON_HOE",
            "GOLD_SWORD", "GOLD_PICKAXE", "GOLD_AXE", "GOLD_SHOVEL", "GOLD_HOE",
            "IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS",
            "GOLDEN_HELMET", "GOLDEN_CHESTPLATE", "GOLDEN_LEGGINGS", "GOLDEN_BOOTS",
            "SHIELD", "BOW", "ARROW", "TIPPED_ARROW", "FISHING_ROD", "CARROT_ON_A_STICK", "WARPED_FUNGUS_ON_A_STICK",
            "FLINT_AND_STEEL", "FIRE_CHARGE", "FIREWORK_STAR", "FIREWORK_ROCKET",
            "SPIDER_EYE", "FERMENTED_SPIDER_EYE", "SLIME_BALL", "SLIME_BLOCK", "MAGMA_CREAM", "BLAZE_ROD", "BLAZE_POWDER",
            "RABBIT_HIDE", "RABBIT_FOOT", "LEATHER",
            "GOLDEN_CARROT", "GOLDEN_APPLE", "PUMPKIN_PIE", "CAKE", "BEETROOT_SOUP", "MUSHROOM_STEW", "RABBIT_STEW", "SUSPICIOUS_STEW",
            "TROPICAL_FISH", "PUFFERFISH", "CHORUS_FRUIT",
            "HONEYCOMB", "HONEYCOMB_BLOCK", "HONEY_BLOCK", "BEEHIVE", "BEE_NEST",
            "MAP", "COMPASS", "CLOCK",
            "BOOK", "PAPER", "BOOKSHELF", "CHISELED_BOOKSHELF", "BUNDLE", "BRUSH",
            "HOPPER", "CHEST", "TRAPPED_CHEST", "BARREL", "DISPENSER", "DROPPER",
            "MINECART", "CHEST_MINECART", "FURNACE_MINECART", "HOPPER_MINECART",
            "POWERED_RAIL", "DETECTOR_RAIL", "ACTIVATOR_RAIL", "TRIPWIRE_HOOK", "LIGHTNING_ROD", "IRON_BARS", "CHAIN", "LANTERN", "SOUL_LANTERN",
            "HEAVY_WEIGHTED_PRESSURE_PLATE", "LIGHT_WEIGHTED_PRESSURE_PLATE",
            "BELL", "GRINDSTONE", "STONECUTTER", "LOOM", "CARTOGRAPHY_TABLE", "FLETCHING_TABLE", "SMITHING_TABLE",
            "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL", "BREWING_STAND", "CAULDRON", "COMPOSTER", "BLAST_FURNACE", "FURNACE", "SMOKER",
            "SHROOMLIGHT", "WEEPING_VINES", "TWISTING_VINES", "CRIMSON_FUNGUS", "WARPED_FUNGUS", "NETHER_WART", "SEA_PICKLE", "SPONGE", "WET_SPONGE",
            "WHITE_BANNER", "ORANGE_BANNER", "MAGENTA_BANNER", "LIGHT_BLUE_BANNER", "YELLOW_BANNER", "LIME_BANNER", "PINK_BANNER",
            "GRAY_BANNER", "LIGHT_GRAY_BANNER", "CYAN_BANNER", "PURPLE_BANNER", "BLUE_BANNER", "BROWN_BANNER", "GREEN_BANNER", "RED_BANNER", "BLACK_BANNER",
            "PAINTING", "ITEM_FRAME", "GLOW_ITEM_FRAME", "ARMOR_STAND",
            "OAK_HANGING_SIGN", "SPRUCE_HANGING_SIGN", "BIRCH_HANGING_SIGN", "JUNGLE_HANGING_SIGN", "ACACIA_HANGING_SIGN",
            "DARK_OAK_HANGING_SIGN", "MANGROVE_HANGING_SIGN", "CHERRY_HANGING_SIGN", "CRIMSON_HANGING_SIGN", "WARPED_HANGING_SIGN",
            "HORSE_ARMOR_LEATHER", "HORSE_ARMOR_IRON",
            "SKELETON_SKULL"
        );

        // RARE: diamond-tier gear, hard-to-farm drops, real "good find" items.
        addToTier(LootTier.RARE,
            "DIAMOND", "EMERALD", "EMERALD_BLOCK",
            "DIAMOND_SWORD", "DIAMOND_PICKAXE", "DIAMOND_AXE", "DIAMOND_SHOVEL", "DIAMOND_HOE",
            "DIAMOND_HELMET", "DIAMOND_CHESTPLATE", "DIAMOND_LEGGINGS", "DIAMOND_BOOTS",
            "CHAINMAIL_HELMET", "CHAINMAIL_CHESTPLATE", "CHAINMAIL_LEGGINGS", "CHAINMAIL_BOOTS",
            "TURTLE_HELMET", "CROSSBOW", "SADDLE", "NAME_TAG", "NETHERITE_SCRAP",
            "ENDER_PEARL", "ENDER_EYE", "GHAST_TEAR", "NAUTILUS_SHELL", "PHANTOM_MEMBRANE",
            "MUSIC_DISC_13", "MUSIC_DISC_CAT", "MUSIC_DISC_BLOCKS", "MUSIC_DISC_CHIRP", "MUSIC_DISC_FAR", "MUSIC_DISC_MALL",
            "MUSIC_DISC_MELLOHI", "MUSIC_DISC_STAL", "MUSIC_DISC_STRAD", "MUSIC_DISC_WARD", "MUSIC_DISC_11", "MUSIC_DISC_WAIT",
            "MUSIC_DISC_OTHERSIDE", "MUSIC_DISC_5", "MUSIC_DISC_PIGSTEP",
            "ENDER_CHEST", "SHULKER_BOX",
            "HORSE_ARMOR_GOLD", "HORSE_ARMOR_DIAMOND",
            "WITHER_SKELETON_SKULL", "ZOMBIE_HEAD", "CREEPER_HEAD",
            "SCULK_SENSOR", "SCULK_CATALYST", "SCULK_SHRIEKER", "CALIBRATED_SCULK_SENSOR",
            "OCHRE_FROGLIGHT", "VERDANT_FROGLIGHT", "PEARLESCENT_FROGLIGHT",
            "RESPAWN_ANCHOR", "END_CRYSTAL"
        );

        // EPIC: the jackpot tier. Netherite gear, elytra, totem, nether star... should feel like a special find.
        addToTier(LootTier.EPIC,
            "NETHERITE_INGOT", "NETHERITE_BLOCK",
            "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_AXE", "NETHERITE_SHOVEL", "NETHERITE_HOE",
            "NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS",
            "ELYTRA", "TRIDENT", "TOTEM_OF_UNDYING",
            "ENCHANTED_GOLDEN_APPLE",
            "NETHER_STAR", "DRAGON_BREATH", "HEART_OF_THE_SEA", "SHULKER_SHELL",
            "BEACON", "CONDUIT", "RECOVERY_COMPASS",
            "DIAMOND_BLOCK", "DRAGON_HEAD"
        );
    }

    /**
     * Adds materials to a given loot tier, silently skipping any name that isn't
     * a valid Material on this server version (keeps the plugin version-portable).
     */
    private void addToTier(LootTier tier, String... materialNames) {
        List<Material> list = materialsByTier.computeIfAbsent(tier, t -> new ArrayList<>());
        for (String name : materialNames) {
            try {
                list.add(Material.valueOf(name));
            } catch (IllegalArgumentException e) {
                // Material doesn't exist in this version, skip it
            }
        }
    }

    private void startSpawnTask() {
        // Cancel existing task if any
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        
        // Schedule next spawn with random delay (25-35 minutes)
        scheduleNextSpawn();
    }
    
    private void scheduleNextSpawn() {
        if (spawnTask != null) {
            spawnTask.cancel();
        }
        
        // Random delay between 20 and 35 minutes
        int minutes = 20 + random.nextInt(16); // 20 to 35
        long delay = 20L * 60 * minutes;
        
        spawnTask = Bukkit.getScheduler().runTaskLater(this, () -> {
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (!players.isEmpty()) {
                spawnRandomChest(players);
            }
            // Schedule next spawn
            scheduleNextSpawn();
        }, delay);
    }

    private void spawnRandomChest(List<Player> players) {
        if (!isSpawning.compareAndSet(false, true)) {
            return;
        }

        try {
            World world = players.get(0).getWorld();

            // Calculate the center point of all connected players
            Location center = calculatePlayersCenter(players);
            
            // Calculate the maximum distance from center to any player
            double maxPlayerDistance = calculateMaxPlayerDistance(center, players);
            
            // Ensure minimum distance of 200 blocks, max of 2000 (or player spread if larger)
            double spawnDistance = Math.max(200, Math.min(2000, maxPlayerDistance));

            // Add some randomness to the angle
            double angle = random.nextDouble() * 2 * Math.PI;
            double offsetX = spawnDistance * Math.cos(angle);
            double offsetZ = spawnDistance * Math.sin(angle);

            // Calculate target coordinates and clamp to world bounds
            double targetX = center.getX() + offsetX;
            double targetZ = center.getZ() + offsetZ;
            
            // Clamp to world boundaries (-10000 to 10000)
            targetX = Math.max(-10000, Math.min(10000, targetX));
            targetZ = Math.max(-10000, Math.min(10000, targetZ));

            int finalX = (int) Math.floor(targetX);
            int finalZ = (int) Math.floor(targetZ);

            // Find the highest solid block at this X,Z coordinate and place chest on top
            Location chestLocation = findHighestBlockOnGround(world, finalX, finalZ);

            if (chestLocation == null) {
                getLogger().warning("Could not find a valid location for chest spawn.");
                return;
            }

            Block chestBlock = chestLocation.getBlock();
            chestBlock.setType(Material.CHEST);

            if (chestBlock.getState() instanceof Chest chest) {
                fillChestWithRandomItems(chest);
                
                // Track this chest as not yet looted
                spawnedChests.put(chestLocation, false);
                
                announceChestSpawn(chestLocation, players);
            }
        } finally {
            isSpawning.set(false);
        }
    }

    private Location calculatePlayersCenter(List<Player> players) {
        double sumX = 0, sumZ = 0;
        for (Player player : players) {
            sumX += player.getLocation().getX();
            sumZ += player.getLocation().getZ();
        }
        double centerX = sumX / players.size();
        double centerZ = sumZ / players.size();
        
        Location firstPlayerLoc = players.get(0).getLocation();
        return new Location(firstPlayerLoc.getWorld(), centerX, firstPlayerLoc.getY(), centerZ);
    }

    private double calculateMaxPlayerDistance(Location center, List<Player> players) {
        double maxDistance = 0;
        for (Player player : players) {
            double dx = player.getLocation().getX() - center.getX();
            double dz = player.getLocation().getZ() - center.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            maxDistance = Math.max(maxDistance, distance);
        }
        return maxDistance;
    }

    private Location findHighestBlockOnGround(World world, int x, int z) {
        // Search from world height down to find the highest solid block
        int maxHeight = world.getMaxHeight() - 1;
        
        for (int y = maxHeight; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (isSolidGroundBlock(block)) {
                // Return the location on top of this block (chest sits on it, not inside)
                return new Location(world, x, y + 1, z);
            }
        }
        return null;
    }

    private boolean isSolidGroundBlock(Block block) {
        Material type = block.getType();
        // Only allow blocks that can have things placed on top and are natural ground blocks
        return type.isSolid() && 
               type != Material.BEDROCK && 
               type != Material.BARRIER &&
               type != Material.LAVA &&
               type != Material.WATER &&
               type != Material.GLASS &&
               type != Material.GLASS_PANE &&
               !type.name().contains("FENCE") &&
               !type.name().contains("WALL");
    }

    private void fillChestWithRandomItems(Chest chest) {
        int itemCount = 5 + random.nextInt(4); // 5 to 8 distinct stacks
        List<Integer> usedSlots = new ArrayList<>();

        // Per-chest counters so a single chest can't roll a pile of top-tier items
        int epicCount = 0;
        int rareCount = 0;

        for (int i = 0; i < itemCount; i++) {
            LootTier tier = rollTier();

            // Anti-abuse: downgrade the tier if this chest already hit its cap for it
            if (tier == LootTier.EPIC && epicCount >= MAX_EPIC_PER_CHEST) {
                tier = LootTier.RARE;
            }
            if (tier == LootTier.RARE && rareCount >= MAX_RARE_PER_CHEST) {
                tier = LootTier.UNCOMMON;
            }

            List<Material> pool = materialsByTier.get(tier);
            if (pool == null || pool.isEmpty()) {
                continue; // safety net, shouldn't normally happen
            }

            Material material = pool.get(random.nextInt(pool.size()));

            if (tier == LootTier.EPIC) {
                epicCount++;
            } else if (tier == LootTier.RARE) {
                rareCount++;
            }

            // Roll a quantity for this tier, then clamp to what the item can actually stack to
            int amount = tier.minAmount + random.nextInt(tier.maxAmount - tier.minAmount + 1);
            amount = Math.max(1, Math.min(amount, material.getMaxStackSize()));

            // Find a random empty slot (0-26 for chest inventory)
            int slot;
            do {
                slot = random.nextInt(27); // Single chest has 27 slots
            } while (usedSlots.contains(slot));
            usedSlots.add(slot);

            chest.getInventory().setItem(slot, new ItemStack(material, amount));
        }
    }

    /**
     * Rolls a loot tier according to the relative weights defined on LootTier.
     * COMMON is heavily favored; EPIC is a small but real chance.
     */
    private LootTier rollTier() {
        int totalWeight = 0;
        for (LootTier tier : LootTier.values()) {
            totalWeight += tier.weight;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (LootTier tier : LootTier.values()) {
            cumulative += tier.weight;
            if (roll < cumulative) {
                return tier;
            }
        }
        return LootTier.COMMON; // fallback, should never be reached
    }

    private void announceChestSpawn(Location location, List<Player> players) {
        String coords = String.format("(%.0f, %d, %.0f)",
            location.getX(), location.getBlockY(), location.getZ());
        
        String message = "§6[Coffre Aleatoire] §aUn coffre aleatoire est apparait en §e" + coords + " §a dans le monde §b" 
            + location.getWorld().getName() + "§a!";
        
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.size() == 1) {
            // Start the spawn timer (first spawn after 15 min, then every 30 min)
            startSpawnTask();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // No action needed - cycle continues running
    }
    
    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        // Check if this is a chest inventory
        if (event.getInventory().getHolder() instanceof Chest chest) {
            Location chestLocation = chest.getLocation();
            
            // Check if this is one of our spawned chests and not already marked as looted
            if (spawnedChests.containsKey(chestLocation) && !spawnedChests.get(chestLocation)) {
                // Check if the chest is empty
                boolean isEmpty = true;
                for (ItemStack item : event.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        isEmpty = false;
                        break;
                    }
                }
                
                if (isEmpty) {
                    // Mark as looted
                    spawnedChests.put(chestLocation, true);
                    
                    // Remove the chest after a short delay (1 second)
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        Block block = chestLocation.getBlock();
                        if (block.getType() == Material.CHEST) {
                            block.setType(Material.AIR);
                            spawnedChests.remove(chestLocation);
                            
                            // Notify ALL connected players
                            String coords = String.format("(%.0f, %d, %.0f)",
                                chestLocation.getX(), chestLocation.getBlockY(), chestLocation.getZ());
                            String message = "§6[Coffre Aleatoire] §7Un coffre a ete vide et a disparu en §e" + coords + "§7!";
                            
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.getWorld().equals(chestLocation.getWorld())) {
                                    player.sendMessage(message);
                                }
                            }
                        }
                    }, 20L); // 20 ticks = 1 second
                }
            }
        }
    }
}
